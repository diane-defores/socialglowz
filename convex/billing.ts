import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import type { MutationCtx, QueryCtx } from "./_generated/server";
import type { Id } from "./_generated/dataModel";
import { requireAuthUserId } from "./authHelpers";

const PRODUCT_SOCIALGLOWZ = "socialglowz";
const PLAN_FREE = "free";
const PLAN_FOUNDER_LTD = "founder_ltd";

const ACTIVE_LEGACY_PLANS = new Set(["pro", "team"]);

declare const process: {
  env: Record<string, string | undefined>;
};

function normalizeCode(code: string) {
  return code.trim().toUpperCase().replace(/\s+/g, "-");
}

function requireAdminSecret(secret: string) {
  const configured = process.env.SOCIALGLOWZ_BILLING_ADMIN_SECRET;
  if (!configured || secret !== configured) {
    throw new Error("Unauthorized billing admin action");
  }
}

async function getActiveEntitlement(
  ctx: QueryCtx | MutationCtx,
  userId: Id<"users">,
  productId: string,
) {
  const entitlements = await ctx.db
    .query("entitlements")
    .withIndex("by_user_product", (q) => q.eq("userId", userId).eq("productId", productId))
    .collect();
  const now = Date.now();
  return (
    entitlements.find(
      (entitlement) =>
        entitlement.status === "active" &&
        (entitlement.expiresAt === undefined || entitlement.expiresAt > now),
    ) ?? null
  );
}

export const getProductAccess = query({
  args: {
    productId: v.optional(v.string()),
  },
  handler: async (ctx, args) => {
    const userId = await requireAuthUserId(ctx);
    const productId = args.productId ?? PRODUCT_SOCIALGLOWZ;
    const entitlement = await getActiveEntitlement(ctx, userId, productId);

    if (entitlement) {
      return {
        productId,
        planId: entitlement.planId,
        status: "active" as const,
        source: entitlement.source,
        entitlementId: entitlement._id,
        expiresAt: entitlement.expiresAt,
        legacyFallback: false,
      };
    }

    const legacySubscription = await ctx.db
      .query("subscriptions")
      .withIndex("by_userId", (q) => q.eq("userId", userId))
      .unique();

    if (
      legacySubscription &&
      legacySubscription.status === "active" &&
      ACTIVE_LEGACY_PLANS.has(legacySubscription.plan) &&
      (legacySubscription.expiresAt === undefined || legacySubscription.expiresAt > Date.now())
    ) {
      return {
        productId,
        planId: legacySubscription.plan,
        status: "active" as const,
        source: "legacy_subscription" as const,
        entitlementId: null,
        expiresAt: legacySubscription.expiresAt,
        legacyFallback: true,
      };
    }

    return {
      productId,
      planId: PLAN_FREE,
      status: "free" as const,
      source: "default" as const,
      entitlementId: null,
      expiresAt: null,
      legacyFallback: false,
    };
  },
});

export const redeemCode = mutation({
  args: {
    code: v.string(),
  },
  handler: async (ctx, args) => {
    const userId = await requireAuthUserId(ctx);
    const code = normalizeCode(args.code);
    if (!code) {
      throw new Error("Redemption code is required");
    }

    const redemptionCode = await ctx.db
      .query("redemptionCodes")
      .withIndex("by_code", (q) => q.eq("code", code))
      .unique();

    if (!redemptionCode) {
      throw new Error("Redemption code not found");
    }
    if (redemptionCode.status === "disabled") {
      throw new Error("Redemption code is disabled");
    }
    if (redemptionCode.status === "redeemed" && redemptionCode.redeemedBy !== userId) {
      throw new Error("Redemption code has already been used");
    }

    const existing = await getActiveEntitlement(ctx, userId, redemptionCode.productId);
    if (redemptionCode.status === "redeemed" && redemptionCode.redeemedBy === userId && existing) {
      return {
        productId: redemptionCode.productId,
        planId: existing.planId,
        status: "active" as const,
        entitlementId: existing._id,
        alreadyRedeemed: true,
      };
    }

    const now = Date.now();
    let entitlementId = existing?._id;

    if (existing) {
      await ctx.db.patch(existing._id, {
        planId: redemptionCode.planId,
        source: redemptionCode.source,
        externalOrderId: redemptionCode.externalOrderId,
        updatedAt: now,
      });
    } else {
      entitlementId = await ctx.db.insert("entitlements", {
        userId,
        productId: redemptionCode.productId,
        planId: redemptionCode.planId,
        status: "active",
        source: redemptionCode.source,
        externalOrderId: redemptionCode.externalOrderId,
        startsAt: now,
        createdAt: now,
        updatedAt: now,
        metadata: {
          redemptionCodeId: redemptionCode._id,
        },
      });
    }

    await ctx.db.patch(redemptionCode._id, {
      status: "redeemed",
      redeemedAt: now,
      redeemedBy: userId,
      updatedAt: now,
    });

    await ctx.db.insert("billingEvents", {
      userId,
      productId: redemptionCode.productId,
      planId: redemptionCode.planId,
      source: redemptionCode.source,
      eventType: "redemption_code_redeemed",
      externalOrderId: redemptionCode.externalOrderId,
      redemptionCodeId: redemptionCode._id,
      entitlementId,
      createdAt: now,
    });

    return {
      productId: redemptionCode.productId,
      planId: redemptionCode.planId,
      status: "active" as const,
      entitlementId,
      alreadyRedeemed: false,
    };
  },
});

export const adminUpsertRedemptionCode = mutation({
  args: {
    adminSecret: v.string(),
    code: v.string(),
    productId: v.optional(v.string()),
    planId: v.optional(v.string()),
    source: v.optional(v.union(v.literal("appsumo"), v.literal("manual"))),
    status: v.optional(v.union(v.literal("available"), v.literal("disabled"))),
    externalOrderId: v.optional(v.string()),
    note: v.optional(v.string()),
  },
  handler: async (ctx, args) => {
    requireAdminSecret(args.adminSecret);

    const code = normalizeCode(args.code);
    if (!code) {
      throw new Error("Redemption code is required");
    }

    const now = Date.now();
    const existing = await ctx.db
      .query("redemptionCodes")
      .withIndex("by_code", (q) => q.eq("code", code))
      .unique();

    if (existing?.status === "redeemed") {
      throw new Error("Cannot update a redeemed code");
    }

    const payload = {
      code,
      productId: args.productId ?? PRODUCT_SOCIALGLOWZ,
      planId: args.planId ?? PLAN_FOUNDER_LTD,
      source: args.source ?? "appsumo",
      status: args.status ?? "available",
      externalOrderId: args.externalOrderId,
      note: args.note,
      updatedAt: now,
    } as const;

    if (existing) {
      await ctx.db.patch(existing._id, payload);
      return { redemptionCodeId: existing._id, created: false };
    }

    const redemptionCodeId = await ctx.db.insert("redemptionCodes", {
      ...payload,
      createdAt: now,
    });

    await ctx.db.insert("billingEvents", {
      productId: payload.productId,
      planId: payload.planId,
      source: payload.source,
      eventType: "redemption_code_created",
      externalOrderId: payload.externalOrderId,
      redemptionCodeId,
      createdAt: now,
    });

    return { redemptionCodeId, created: true };
  },
});

import { convexTest } from "convex-test";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import schema from "./schema";
import { modules } from "./test.setup";

const authState = vi.hoisted(() => ({
  userId: "",
  authenticated: true,
}));

vi.mock("./authHelpers", () => ({
  requireAuthUserId: vi.fn(async () => {
    if (!authState.authenticated) {
      throw new Error("Not authenticated");
    }
    return authState.userId;
  }),
}));

beforeEach(() => {
  authState.userId = "";
  authState.authenticated = true;
  process.env.SOCIALGLOWZ_BILLING_ADMIN_SECRET = "test-secret";
});

describe("billing entitlement foundation", () => {
  it("creates lifetime deal redemption codes through the admin-secret mutation", async () => {
    const t = convexTest(schema, modules);

    await expect(
      t.mutation(api.billing.adminUpsertRedemptionCode, {
        adminSecret: "wrong",
        code: "APPSUMO-001",
      }),
    ).rejects.toThrow(/unauthorized/i);

    const result = await t.mutation(api.billing.adminUpsertRedemptionCode, {
      adminSecret: "test-secret",
      code: " ltd 001 ",
      externalOrderId: "order-1",
    });

    expect(result.created).toBe(true);

    const code = await t.run((ctx) =>
      ctx.db
        .query("redemptionCodes")
        .withIndex("by_code", (q) => q.eq("code", "LTD-001"))
        .unique(),
    );
    expect(code?.productId).toBe("socialglowz");
    expect(code?.planId).toBe("lifetime_deal");
    expect(code?.source).toBe("manual");
  });

  it("redeems one code into an active entitlement and billing event", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) => ctx.db.insert("users", { createdAt: Date.now() }));
    authState.userId = userId;

    await t.mutation(api.billing.adminUpsertRedemptionCode, {
      adminSecret: "test-secret",
      code: "LTD-002",
      externalOrderId: "order-2",
    });

    const redemption = await t.mutation(api.billing.redeemCode, {
      code: "ltd-002",
    });

    expect(redemption).toMatchObject({
      productId: "socialglowz",
      planId: "lifetime_deal",
      status: "active",
      alreadyRedeemed: false,
    });

    const access = await t.query(api.billing.getProductAccess, {});
    expect(access).toMatchObject({
      productId: "socialglowz",
      planId: "lifetime_deal",
      status: "active",
      source: "manual",
      legacyFallback: false,
    });

    const [entitlements, events] = await Promise.all([
      t.run((ctx) => ctx.db.query("entitlements").collect()),
      t.run((ctx) => ctx.db.query("billingEvents").collect()),
    ]);

    expect(entitlements).toHaveLength(1);
    expect(events.some((event) => event.eventType === "redemption_code_redeemed")).toBe(true);
  });

  it("keeps repeat redemption idempotent for the same user but blocks another user", async () => {
    const t = convexTest(schema, modules);
    const userA = await t.run((ctx) => ctx.db.insert("users", { createdAt: Date.now() }));
    const userB = await t.run((ctx) => ctx.db.insert("users", { createdAt: Date.now() }));

    await t.mutation(api.billing.adminUpsertRedemptionCode, {
      adminSecret: "test-secret",
      code: "APPSUMO-003",
    });

    authState.userId = userA;
    await t.mutation(api.billing.redeemCode, { code: "APPSUMO-003" });
    const repeat = await t.mutation(api.billing.redeemCode, { code: "APPSUMO-003" });
    expect(repeat.alreadyRedeemed).toBe(true);

    authState.userId = userB;
    await expect(
      t.mutation(api.billing.redeemCode, { code: "APPSUMO-003" }),
    ).rejects.toThrow(/already been used/i);

    const entitlements = await t.run((ctx) => ctx.db.query("entitlements").collect());
    expect(entitlements).toHaveLength(1);
  });

  it("falls back to legacy active subscription until the UI migrates", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) => ctx.db.insert("users", { createdAt: Date.now() }));
    authState.userId = userId;

    await t.run((ctx) =>
      ctx.db.insert("subscriptions", {
        userId,
        plan: "pro",
        status: "active",
      }),
    );

    const access = await t.query(api.billing.getProductAccess, {});

    expect(access).toMatchObject({
      productId: "socialglowz",
      planId: "pro",
      status: "active",
      source: "legacy_subscription",
      legacyFallback: true,
    });
  });
});

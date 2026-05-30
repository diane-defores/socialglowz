import { convexTest } from "convex-test";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import schema from "./schema";
import { modules } from "./test.setup";

const authState = vi.hoisted(() => ({
  userId: "",
  authenticated: true,
}));

let fetchMock: ReturnType<typeof vi.fn>;

vi.mock("./authHelpers", () => ({
  requireAuthUserId: vi.fn(async () => {
    if (!authState.authenticated) {
      throw new Error("Not authenticated");
    }
    return authState.userId;
  }),
}));

function mockFetchResponse(status: number, body: unknown) {
  fetchMock.mockResolvedValue(
    new Response(JSON.stringify(body), { status }),
  );
}

beforeEach(() => {
  authState.userId = "";
  authState.authenticated = true;
  process.env.SOCIALGLOWZ_BILLING_ADMIN_SECRET = "test-secret";
  process.env.SOCIALGLOWZ_SUITE_BRIDGE_SECRET = "suite-secret";
  process.env.SOCIALGLOWZ_SUITE_BRIDGE_URL =
    "https://suite.example/api/bridge/socialglowz";

  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("billing bridge adapter", () => {
  it("rejects admin operations with invalid billing secret", async () => {
    const t = convexTest(schema, modules);

    await expect(
      t.action(api.billing.adminUpsertRedemptionCode, {
        adminSecret: "wrong",
        code: "APPSUMO-001",
      }),
    ).rejects.toThrow(/Unauthorized billing admin action/i);
  });

  it("fails closed when suite bridge URL is missing", async () => {
    delete process.env.SOCIALGLOWZ_SUITE_BRIDGE_URL;
    const t = convexTest(schema, modules);

    await expect(
      t.action(api.billing.getProductAccess, {}),
    ).rejects.toThrow(/suite_bridge_not_configured/i);
  });

  it("reads free access from suite snapshot", async () => {
    const t = convexTest(schema, modules);
    mockFetchResponse(200, {
      status: "ok",
      snapshot: {
        hasAccess: false,
        globalUserId: "gu-1",
        planId: null,
        source: null,
        reasonCode: "missing_product_entitlement",
      },
    });

    const access = await t.action(api.billing.getProductAccess, {});

    expect(access).toMatchObject({
      productId: "socialglowz",
      planId: "free",
      status: "free",
      source: "default",
      legacyFallback: false,
    });
  });

  it("reads active suite access", async () => {
    const t = convexTest(schema, modules);
    mockFetchResponse(200, {
      status: "ok",
      snapshot: {
        hasAccess: true,
        globalUserId: "gu-1",
        planId: "lifetime_deal",
        source: "manual",
        reasonCode: "active_entitlement",
      },
    });

    const access = await t.action(api.billing.getProductAccess, {});

    expect(access).toMatchObject({
      productId: "socialglowz",
      planId: "lifetime_deal",
      status: "active",
      source: "manual",
      reasonCode: "active_entitlement",
      legacyFallback: false,
    });
  });

  it("redeems code for suite-backed entitlement", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );
    authState.userId = userId;

    mockFetchResponse(200, {
      status: "ok",
      redemption: {
        hasAccess: true,
        planId: "lifetime_deal",
        source: "manual",
      },
    });

    const redemption = await t.action(api.billing.redeemCode, {
      code: "LTD-007",
    });

    expect(redemption).toMatchObject({
      status: "active",
      alreadyRedeemed: false,
      planId: "lifetime_deal",
    });
  });

  it("marks same-user code redemption as idempotent", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );
    authState.userId = userId;

    mockFetchResponse(200, {
      status: "ok",
      redemption: {
        hasAccess: true,
        planId: "lifetime_deal",
        source: "manual",
        alreadyRedeemed: true,
      },
    });

    const redemption = await t.action(api.billing.redeemCode, {
      code: "LTD-007",
    });

    expect(redemption).toMatchObject({
      status: "active",
      alreadyRedeemed: true,
    });
  });

  it("prevents second-user code reuse at the bridge layer", async () => {
    const t = convexTest(schema, modules);
    const userA = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );
    const userB = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );

    authState.userId = userA;
    mockFetchResponse(200, {
      status: "ok",
      redemption: {
        hasAccess: true,
        planId: "lifetime_deal",
        source: "manual",
        alreadyRedeemed: false,
      },
    });
    await t.action(api.billing.redeemCode, { code: "LTD-007" });

    authState.userId = userB;
    mockFetchResponse(409, {
      status: "error",
      error: "code_already_used",
    });

    await expect(
      t.action(api.billing.redeemCode, { code: "LTD-007" }),
    ).rejects.toThrow(/used/i);
  });

  it("maps suite-level missing code failures", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );
    authState.userId = userId;

    mockFetchResponse(400, {
      status: "error",
      error: "code_not_found",
    });

    await expect(
      t.action(api.billing.redeemCode, { code: "MISSING-1" }),
    ).rejects.toThrow(/not_found/i);
  });

  it("supports suite upsert as an admin operation", async () => {
    const t = convexTest(schema, modules);
    mockFetchResponse(200, {
      status: "ok",
      result: {
        created: true,
      },
    });

    const response = await t.action(api.billing.adminUpsertRedemptionCode, {
      adminSecret: "test-secret",
      code: " ltd 007 ",
      status: "disabled",
    });

    expect(response).toMatchObject({ created: true });
  });

  it("supports manual grant and revoke/refund parity operations", async () => {
    const t = convexTest(schema, modules);

    mockFetchResponse(200, {
      status: "ok",
      result: {
        alreadyGranted: false,
        status: "ok",
      },
    });
    const grant = await t.action(
      api.billing.adminManualGrantSocialGlowzAccess,
      {
        adminSecret: "test-secret",
        providerAccountId: "provider-1",
        planId: "lifetime_deal",
      },
    );
    expect(grant.alreadyActive).toBe(false);

    mockFetchResponse(200, {
      status: "ok",
      result: {
        status: "already_revoked",
      },
    });
    const revoke = await t.action(api.billing.adminRevokeSocialGlowzAccess, {
      adminSecret: "test-secret",
      providerAccountId: "provider-1",
    });
    expect(revoke.alreadyRevoked).toBe(true);

    mockFetchResponse(200, {
      status: "ok",
      result: {
        status: "ok",
      },
    });
    const refund = await t.action(api.billing.adminRefundSocialGlowzAccess, {
      adminSecret: "test-secret",
      providerAccountId: "provider-1",
    });
    expect(refund.alreadyRevoked).toBe(false);
  });

  it("returns a safe local migration summary without destructive operations", async () => {
    const t = convexTest(schema, modules);
    const userId = await t.run((ctx) =>
      ctx.db.insert("users", { createdAt: Date.now() }),
    );
    const now = Date.now();

    await t.run((ctx) =>
      ctx.db.insert("entitlements", {
        userId,
        productId: "socialglowz",
        planId: "lifetime_deal",
        status: "active",
        source: "manual",
        sourceEventId: "evt-1",
        startsAt: now - 10_000,
        createdAt: now - 10_000,
        updatedAt: now,
      }),
    );
    await t.run((ctx) =>
      ctx.db.insert("redemptionCodes", {
        code: "LTD-L1",
        productId: "socialglowz",
        planId: "lifetime_deal",
        source: "manual",
        status: "available",
        createdAt: now - 5_000,
        updatedAt: now,
      }),
    );
    await t.run((ctx) =>
      ctx.db.insert("billingEvents", {
        userId,
        productId: "socialglowz",
        planId: "lifetime_deal",
        source: "manual",
        eventType: "relic_import",
        createdAt: now - 1_000,
      }),
    );
    await t.run((ctx) =>
      ctx.db.insert("subscriptions", {
        userId,
        plan: "free",
        status: "active",
      }),
    );

    const summary = await t.query(api.billing.getLocalBillingMigrationSummary, {
      adminSecret: "test-secret",
    });

    expect(summary.status).toBe("local_compat_only");
    expect(summary.totals.entitlements).toBe(1);
    expect(summary.totals.redemptionCodes).toBe(1);
    expect(summary.totals.billingEvents).toBe(1);
    expect(summary.totals.subscriptions).toBe(1);
  });
});

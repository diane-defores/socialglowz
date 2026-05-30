import { action, query } from './_generated/server'
import { v } from 'convex/values'
import { requireAuthUserId } from './authHelpers'

/**
 * Suite entitlement bridge adapter.
 * - Canonical entitlement state is owned by WinFlowz suite.
 * - Local tables are retained only as migration/cached compatibility surfaces.
 */

const PRODUCT_SOCIALGLOWZ = 'socialglowz'
const PLAN_FREE = 'free'
const PLAN_LIFETIME_DEAL = 'lifetime_deal'
const PLAN_FOUNDER_LTD = 'founder_ltd'

type EntitlementSnapshot = {
  hasAccess: boolean
  globalUserId: string | null
  planId: string | null
  source: string | null
  reasonCode: string
}

type BridgeResponseOk<T extends Record<string, unknown> = Record<string, unknown>> = {
  status: 'ok'
  snapshot?: EntitlementSnapshot
  redemption?: {
    hasAccess: boolean
    planId: string | null
    source: string | null
    reasonCode?: string
    alreadyRedeemed?: boolean
  }
  result?: T
}

type BridgeResponseFailure = {
  status: 'error' | 'unavailable'
  error: string
}

declare const process: {
  env: Record<string, string | undefined>
}

function requireAdminSecret(secret: string) {
  const configured = process.env.SOCIALGLOWZ_BILLING_ADMIN_SECRET
  if (!configured || secret !== configured) {
    throw new Error('Unauthorized billing admin action')
  }
}

function getSuiteBridgeUrl(raw: string | undefined): string {
  if (!raw) {
    throw new Error('suite_bridge_not_configured')
  }

  const normalized = raw.trim().replace(/\/$/, '')
  if (!normalized) {
    throw new Error('suite_bridge_not_configured')
  }

  if (normalized.endsWith('/api/bridge/socialglowz')) {
    return normalized
  }

  return `${normalized}/api/bridge/socialglowz`
}

function getSuiteBridgeSecret() {
  const secret = process.env.SOCIALGLOWZ_SUITE_BRIDGE_SECRET
  if (!secret) {
    throw new Error('suite_bridge_not_configured')
  }
  return secret
}

function normalizeCode(code: string) {
  return code.trim().toUpperCase().replace(/\s+/g, '-')
}

function mapBridgeError(message: string) {
  if (/invalid_payload|missing|provider_account_id_required|code_required/i.test(message)) {
    return 'invalid_payload'
  }
  if (/code_not_found/i.test(message)) return 'not_found'
  if (/code_disabled|already_disabled/i.test(message)) return 'disabled'
  if (/code_already_used|already_redeemed/i.test(message)) return 'used'
  if (/unauthorized|not authenticated|authentication/i.test(message)) {
    return 'unauthorized'
  }
  if (/not_configured|unavailable|failed|bridge_secret_not_configured|invalid_socialglowz_bridge_secret|bridge_secret_mismatch/i.test(message)) {
    return 'bridge_not_configured'
  }
  return 'generic'
}

function normalizePlan(planId?: string | null) {
  return planId || PLAN_LIFETIME_DEAL
}

function isAllowedPlanForSocialGlowz(planId: string) {
  return planId === PLAN_LIFETIME_DEAL || planId === PLAN_FOUNDER_LTD
}

type SuiteBridgeArgs = {
  operation:
    | 'snapshot'
    | 'redeem_code'
    | 'manual_grant'
    | 'revoke'
    | 'refund'
    | 'disable_code'
    | 'upsert_code'
  providerAccountId?: string
  code?: string
  plan?: string
  source?: string
  reason?: string
  email?: string
  sourceRef?: string
  status?: string
}

async function callSuiteBridge<T extends Record<string, unknown> = Record<string, unknown>>(
  args: SuiteBridgeArgs,
): Promise<BridgeResponseOk<T>> {
  const suiteBridgeUrl = getSuiteBridgeUrl(process.env.SOCIALGLOWZ_SUITE_BRIDGE_URL)
  const suiteBridgeSecret = getSuiteBridgeSecret()

  let response: Response
  try {
    response = await fetch(suiteBridgeUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-socialglowz-suite-secret': suiteBridgeSecret,
      },
      body: JSON.stringify({
        ...args,
        email: args.email,
        sourceRef: args.sourceRef,
      }),
    })
  } catch (error) {
    throw new Error(`bridge_request_failed: ${error instanceof Error ? error.message : 'network_error'}`)
  }

  let payload: BridgeResponseOk<T> | BridgeResponseFailure
  try {
    payload = (await response.json()) as BridgeResponseOk<T> | BridgeResponseFailure
  } catch {
    throw new Error('bridge_malformed_response')
  }

  if (!response.ok || payload.status !== 'ok') {
    const mappedError = payload && typeof payload === 'object' && 'error' in payload
      ? mapBridgeError(payload.error)
      : `bridge_http_${response.status}`
    throw new Error(mappedError)
  }

  return payload
}

export const getProductAccess = action({
  args: {
    productId: v.optional(v.string()),
  },
  handler: async (ctx) => {
    const userId = await requireAuthUserId(ctx)
    const response = await callSuiteBridge({
      operation: 'snapshot',
      providerAccountId: userId,
      sourceRef: userId,
    })

    const snapshot = response.snapshot
    if (!snapshot) {
      throw new Error('invalid_snapshot')
    }

    if (!snapshot.hasAccess) {
      return {
        productId: PRODUCT_SOCIALGLOWZ,
        planId: PLAN_FREE,
        status: 'free' as const,
        source: 'default' as const,
        entitlementId: null,
        expiresAt: null,
        legacyFallback: false,
      }
    }

    return {
      productId: PRODUCT_SOCIALGLOWZ,
      planId: snapshot.planId ?? PLAN_FREE,
      status: 'active' as const,
      source: snapshot.source ?? 'manual',
      entitlementId: null,
      expiresAt: null,
      legacyFallback: false,
      reasonCode: snapshot.reasonCode,
    }
  },
})

export const redeemCode = action({
  args: {
    code: v.string(),
  },
  handler: async (ctx, args) => {
    const userId = await requireAuthUserId(ctx)
    const code = normalizeCode(args.code)
    if (!code) {
      throw new Error('code_required')
    }

    const response = await callSuiteBridge({
      operation: 'redeem_code',
      providerAccountId: userId,
      code,
      sourceRef: userId,
    })

    const redemption = response.redemption
    if (!redemption) {
      throw new Error('bridge_response_missing_redemption')
    }

    return {
      productId: PRODUCT_SOCIALGLOWZ,
      planId: redemption.planId ?? PLAN_FREE,
      status: redemption.hasAccess ? ('active' as const) : ('free' as const),
      source: redemption.source ?? 'manual',
      entitlementId: null,
      expiresAt: null,
      alreadyRedeemed: Boolean(redemption.alreadyRedeemed),
      reasonCode: redemption.reasonCode,
    }
  },
})

export const adminUpsertRedemptionCode = action({
  args: {
    adminSecret: v.string(),
    code: v.string(),
    productId: v.optional(v.string()),
    planId: v.optional(v.string()),
    source: v.optional(
      v.union(
        v.literal('appsumo'),
        v.literal('direct'),
        v.literal('legacy'),
        v.literal('manual'),
        v.literal('partner'),
      ),
    ),
    status: v.optional(v.union(v.literal('available'), v.literal('disabled'))),
    sourceRef: v.optional(v.string()),
    externalOrderId: v.optional(v.string()),
    note: v.optional(v.string()),
  },
  handler: async (_ctx, args) => {
    requireAdminSecret(args.adminSecret)
    const code = normalizeCode(args.code)
    if (!code) {
      throw new Error('code_required')
    }
    if (args.productId && args.productId !== PRODUCT_SOCIALGLOWZ) {
      throw new Error('product_not_allowed')
    }
    if (args.planId && !isAllowedPlanForSocialGlowz(normalizePlan(args.planId))) {
      throw new Error('plan_not_allowed')
    }

    const response = await callSuiteBridge<{ created?: boolean }>( {
      operation: 'upsert_code',
      code,
      plan: normalizePlan(args.planId),
      source: args.source,
      status: args.status,
      sourceRef: args.sourceRef ?? args.externalOrderId,
    })

    return {
      created: response.result?.created ?? true,
      suiteResponseStatus: response.status,
      planId: normalizePlan(args.planId),
      source: args.source ?? 'manual',
    }
  },
})

export const adminManualGrantSocialGlowzAccess = action({
  args: {
    adminSecret: v.string(),
    providerAccountId: v.string(),
    planId: v.optional(v.string()),
    source: v.optional(
      v.union(
        v.literal('appsumo'),
        v.literal('direct'),
        v.literal('legacy'),
        v.literal('manual'),
        v.literal('partner'),
      ),
    ),
    sourceRef: v.optional(v.string()),
  },
  handler: async (_ctx, args) => {
    requireAdminSecret(args.adminSecret)
    const planId = normalizePlan(args.planId)
    if (!isAllowedPlanForSocialGlowz(planId)) {
      throw new Error('plan_not_allowed')
    }

    const response = await callSuiteBridge<{ alreadyGranted?: boolean }>(
      {
        operation: 'manual_grant',
        providerAccountId: args.providerAccountId,
        plan: planId,
        source: args.source ?? 'manual',
        sourceRef: args.sourceRef,
      },
    )

    return {
      alreadyActive: response.result?.alreadyGranted ?? false,
      source: response.result?.alreadyGranted ? 'reopened' : 'created',
      suiteResponseStatus: response.status,
    }
  },
})

export const adminRevokeSocialGlowzAccess = action({
  args: {
    adminSecret: v.string(),
    providerAccountId: v.string(),
    sourceRef: v.optional(v.string()),
    reason: v.optional(v.string()),
  },
  handler: async (_ctx, args) => {
    requireAdminSecret(args.adminSecret)
    const response = await callSuiteBridge<{ status?: 'already_revoked' | 'ok'; hasAccess?: boolean }>(
      {
        operation: 'revoke',
        providerAccountId: args.providerAccountId,
        reason: args.reason,
        sourceRef: args.sourceRef,
      },
    )

    return {
      alreadyRevoked: response.result?.status === 'already_revoked',
      suiteResponseStatus: response.status,
      reason: args.reason,
    }
  },
})

export const adminRefundSocialGlowzAccess = action({
  args: {
    adminSecret: v.string(),
    providerAccountId: v.string(),
    sourceRef: v.optional(v.string()),
    reason: v.optional(v.string()),
  },
  handler: async (_ctx, args) => {
    requireAdminSecret(args.adminSecret)
    const response = await callSuiteBridge<{ status?: 'already_revoked' | 'ok'; hasAccess?: boolean }>(
      {
        operation: 'refund',
        providerAccountId: args.providerAccountId,
        reason: args.reason,
        sourceRef: args.sourceRef,
      },
    )

    return {
      alreadyRevoked: response.result?.status === 'already_revoked',
      suiteResponseStatus: response.status,
      reason: args.reason,
    }
  },
})

export const getLocalBillingMigrationSummary = query({
  args: {
    adminSecret: v.string(),
  },
  handler: async (ctx, args) => {
    requireAdminSecret(args.adminSecret)

    const [entitlements, redemptionCodes, billingEvents, subscriptions] = await Promise.all(
      [
        ctx.db.query('entitlements').collect(),
        ctx.db.query('redemptionCodes').collect(),
        ctx.db.query('billingEvents').collect(),
        ctx.db.query('subscriptions').collect(),
      ],
    )

    return {
      status: 'local_compat_only',
      totals: {
        entitlements: entitlements.length,
        redemptionCodes: redemptionCodes.length,
        billingEvents: billingEvents.length,
        subscriptions: subscriptions.length,
      },
      source: {
        entitlements: entitlements.map((entry) => ({
          planId: entry.planId,
          status: entry.status,
          productId: entry.productId,
        })),
        redemptionCodes: redemptionCodes.map((entry) => ({
          status: entry.status,
          source: entry.source,
          planId: entry.planId,
        })),
        billingEvents: billingEvents.slice(0, 25).map((entry) => ({
          productId: entry.productId,
          eventType: entry.eventType,
          planId: entry.planId ?? null,
        })),
      },
      migrationNotes: {
        notes: [
          'Local tables are read-only migration inputs; no deletions performed.',
          `Entitlements table rows: ${entitlements.length}`,
          `Redemption code rows: ${redemptionCodes.length}`,
        ],
      },
    }
  },
})

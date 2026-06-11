import { describe, expect, it } from 'vitest'
import { getSafeAccessCheckError, getSafeBillingError } from './useBillingAccess'

describe('getSafeBillingError', () => {
  it.each([
    [new Error('code_not_found'), 'billing.errors.not_found'],
    [new Error('Redemption code not found'), 'billing.errors.not_found'],
    [new Error('Redemption code is disabled'), 'billing.errors.disabled'],
    [new Error('Redemption code has already been used'), 'billing.errors.used'],
    [new Error('already_redeemed'), 'billing.errors.used'],
    [new Error('code already used'), 'billing.errors.used'],
    [new Error('Code is required'), 'billing.errors.required'],
    [new Error('Not authenticated'), 'billing.errors.unauthorized'],
    [new Error('SocialGlowz bridge unavailable'), 'billing.errors.bridge_unavailable'],
    [new Error('Suite bridge not configured'), 'billing.errors.bridge_unavailable'],
    [new Error('invalid socialglowz bridge secret'), 'billing.errors.bridge_unavailable'],
    [new Error('Malformed response from bridge'), 'billing.errors.bridge_unavailable'],
    [new Error('Unexpected backend detail'), 'billing.errors.generic'],
  ])('maps %s to %s', (error, key) => {
    expect(getSafeBillingError(error)).toBe(key)
  })
})

describe('getSafeAccessCheckError', () => {
  it('does not expose redemption-code errors during automatic access checks', () => {
    expect(getSafeAccessCheckError(new Error('code_not_found'))).toBe(
      'billing.errors.access_check_failed',
    )
    expect(getSafeAccessCheckError(new Error('invalid code'))).toBe(
      'billing.errors.access_check_failed',
    )
  })

  it('preserves operational access-check errors', () => {
    expect(getSafeAccessCheckError(new Error('Suite bridge not configured'))).toBe(
      'billing.errors.bridge_unavailable',
    )
    expect(getSafeAccessCheckError(new Error('Not authenticated'))).toBe(
      'billing.errors.unauthorized',
    )
  })
})

<template>
  <section class="billing-panel">
    <div class="billing-panel-header">
      <div class="billing-title-row">
        <i class="pi pi-key" />
        <h3>{{ $t('billing.title') }}</h3>
      </div>
      <span
        class="billing-status-pill"
        :class="statusClass"
      >
        {{ statusLabel }}
      </span>
    </div>

    <p class="billing-copy">{{ helperText }}</p>

    <div
      v-if="showPlanDetails"
      class="billing-plan-row"
    >
      <span>{{ $t('billing.current_plan') }}</span>
      <strong>{{ planLabel }}</strong>
    </div>

    <form
      v-if="showRedeemForm"
      class="billing-redeem-form"
      @submit.prevent="submitRedeem"
    >
      <label
        class="billing-input-label"
        for="billing-redemption-code"
      >
        {{ $t('billing.code_label') }}
      </label>
      <div class="billing-input-row">
        <input
          id="billing-redemption-code"
          v-model="redemptionCode"
          class="billing-input"
          type="text"
          autocomplete="one-time-code"
          autocapitalize="characters"
          spellcheck="false"
          :placeholder="$t('billing.code_placeholder')"
          :disabled="inputDisabled"
        />
        <button
          class="billing-submit-btn"
          type="submit"
          :disabled="submitDisabled"
        >
          <i
            v-if="isRedeeming"
            class="pi pi-spin pi-spinner"
          />
          <span>{{ isRedeeming ? $t('billing.redeeming') : $t('billing.redeem_button') }}</span>
        </button>
      </div>
    </form>

    <p
      v-if="successKey"
      class="billing-message success"
    >
      <i class="pi pi-check-circle" />
      {{ $t(successKey) }}
    </p>
    <p
      v-else-if="errorKey"
      class="billing-message error"
    >
      <i class="pi pi-exclamation-circle" />
      {{ $t(errorKey) }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useBillingAccess } from '@/composables/useBillingAccess'

const { t } = useI18n()
const redemptionCode = ref('')
const {
  access,
  canRedeem,
  errorKey,
  isLifetimeDeal,
  isRedeeming,
  redeemCode,
  status,
  successKey,
} = useBillingAccess()

const statusClass = computed(() => ({
  active: status.value === 'active',
  muted:
    status.value === 'unconfigured' ||
    status.value === 'signed_out' ||
    status.value === 'loading' ||
    status.value === 'bridge_unavailable',
  error: status.value === 'error',
}))

const statusLabel = computed(() => {
  if (status.value === 'active') {
        return isLifetimeDeal.value
      ? t('billing.status_lifetime_deal')
      : t('billing.status_active')
  }
  if (status.value === 'loading') return t('billing.status_loading')
  if (status.value === 'bridge_unavailable') return t('billing.status_bridge_unavailable')
  if (status.value === 'signed_out') return t('billing.status_signed_out')
  if (status.value === 'unconfigured') return t('billing.status_unconfigured')
  if (status.value === 'error') return t('billing.status_error')
  return t('billing.status_free')
})

const helperText = computed(() => {
  if (status.value === 'unconfigured') return t('billing.unconfigured_hint')
  if (status.value === 'signed_out') return t('billing.signed_out_hint')
  if (status.value === 'loading') return t('billing.loading_hint')
  if (status.value === 'bridge_unavailable') return t('billing.bridge_unavailable_hint')
  if (status.value === 'active') {
    return isLifetimeDeal.value
      ? t('billing.lifetime_deal_active_hint')
      : t('billing.active_hint')
  }
  return t('billing.free_hint')
})

const planLabel = computed(() => {
  if (isLifetimeDeal.value) return t('billing.plan_lifetime_deal')
  if (access.value?.status === 'active') return t('billing.plan_active')
  return t('billing.plan_free')
})

const showPlanDetails = computed(() => status.value === 'active' || status.value === 'free')
const showRedeemForm = computed(() =>
  status.value !== 'active' && status.value !== 'bridge_unavailable'
)
const inputDisabled = computed(() => !canRedeem.value || isRedeeming.value)
const submitDisabled = computed(
  () => !redemptionCode.value.trim() || inputDisabled.value,
)

async function submitRedeem() {
  const result = await redeemCode(redemptionCode.value)
  if (result?.status === 'active') {
    redemptionCode.value = ''
  }
}
</script>

<style scoped>
.billing-panel {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  padding: 0.95rem;
  margin-bottom: 1rem;
  border: 1px solid color-mix(in srgb, var(--surface-border) 76%, var(--primary-color) 24%);
  border-radius: 12px;
  background: color-mix(in srgb, var(--surface-card) 92%, var(--surface-ground) 8%);
}

.billing-panel-header,
.billing-title-row,
.billing-plan-row,
.billing-input-row,
.billing-message {
  display: flex;
  align-items: center;
}

.billing-panel-header {
  justify-content: space-between;
  gap: 0.75rem;
}

.billing-title-row {
  gap: 0.55rem;
  min-width: 0;
}

.billing-title-row i {
  color: var(--primary-color);
  font-size: 0.95rem;
}

.billing-title-row h3 {
  margin: 0;
  color: var(--text-color);
  font-size: 0.95rem;
  line-height: 1.25;
}

.billing-status-pill {
  flex: 0 0 auto;
  padding: 0.3rem 0.55rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color) 12%, var(--surface-card) 88%);
  color: var(--primary-color);
  font-size: 0.72rem;
  font-weight: 700;
  white-space: nowrap;
}

.billing-status-pill.muted {
  background: rgba(148, 163, 184, 0.16);
  color: var(--text-color-secondary);
}

.billing-status-pill.error {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
}

.billing-copy {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 0.82rem;
  line-height: 1.45;
}

.billing-plan-row {
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--surface-border);
  border-radius: 10px;
  background: var(--surface-ground);
  color: var(--text-color-secondary);
  font-size: 0.82rem;
}

.billing-plan-row strong {
  color: var(--text-color);
  font-size: 0.84rem;
  text-align: right;
}

.billing-redeem-form {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.billing-input-label {
  color: var(--text-color-secondary);
  font-size: 0.8rem;
  font-weight: 600;
}

.billing-input-row {
  gap: 0.55rem;
}

.billing-input {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  padding: 0.62rem 0.75rem;
  border: 1px solid var(--surface-border);
  border-radius: 10px;
  outline: none;
  background: var(--surface-ground);
  color: var(--text-color);
  font-size: 0.9rem;
  box-sizing: border-box;
}

.billing-input:focus {
  border-color: var(--primary-color);
}

.billing-submit-btn {
  flex: 0 0 auto;
  min-height: 2.45rem;
  padding: 0.62rem 0.85rem;
  border: none;
  border-radius: 10px;
  background: var(--primary-color);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
}

.billing-submit-btn:disabled {
  cursor: default;
  opacity: 0.55;
}

.billing-message {
  gap: 0.45rem;
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.billing-message.success {
  color: #15803d;
}

.billing-message.error {
  color: #dc2626;
}

@media (max-width: 520px) {
  .billing-panel {
    padding: 0.9rem;
  }

  .billing-panel-header {
    align-items: flex-start;
  }

  .billing-input-row {
    align-items: stretch;
    flex-direction: column;
  }

  .billing-submit-btn {
    width: 100%;
  }
}
</style>

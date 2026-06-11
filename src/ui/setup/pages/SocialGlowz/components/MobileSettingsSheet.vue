<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div
        v-if="modelValue"
        class="sheet-overlay"
        @click.self="closeSheet"
      >
        <div
          ref="sheetRef"
          class="profile-sheet settings-sheet"
          :class="{ 'is-dark': themeStore.isDarkMode }"
          :style="sheetStyle"
        >
          <div
            class="sheet-drag-zone"
            @pointerdown="onDragStart"
            @pointermove="onDragMove"
            @pointerup="onDragEnd"
            @pointercancel="onDragCancel"
          >
            <div class="sheet-handle" />
            <div class="sheet-header">
              <span class="sheet-title">{{ $t('common.settings') }}</span>
              <button
                class="sheet-close-btn"
                @click="closeSheet"
              >
                <i class="pi pi-times" />
              </button>
            </div>
          </div>

          <div class="settings-content">
            <!-- Account + backup section -->
            <p class="settings-section-label">{{ $t('account.section_title') }}</p>
            <div class="settings-account-card">
              <div class="settings-account-card-header">
                <div>
                  <p class="settings-account-hint">
                    {{ isSignedIn && nudge.hasEmailAccount.value
                      ? $t('account.signed_in_hint')
                      : !isConvexConfigured
                        ? $t('account.unavailable_hint')
                        : $t('account.auth_hint') }}
                  </p>
                </div>
              </div>

              <div class="settings-account-actions-row">
                <span
                  class="settings-account-status"
                  :class="{ connected: isSignedIn && nudge.hasEmailAccount.value }"
                >
                  {{ isSignedIn && nudge.hasEmailAccount.value
                    ? $t('account.connected_status')
                    : $t('account.disconnected_status') }}
                </span>
                <button
                  type="button"
                  class="settings-sync-toggle"
                  @click="syncInfoExpanded = !syncInfoExpanded"
                >
                  <span>
                    {{ syncInfoExpanded ? $t('account.sync_less') : $t('account.sync_more') }}
                  </span>
                  <i
                    class="pi"
                    :class="syncInfoExpanded ? 'pi-chevron-up' : 'pi-chevron-down'"
                  />
                </button>
              </div>

              <div
                v-if="syncInfoExpanded"
                class="settings-sync-info-box"
              >
                <div class="settings-sync-info-row">
                  <i class="pi pi-cloud" />
                  <p>{{ $t('account.sync_info') }}</p>
                </div>
                <div class="settings-sync-warning-row">
                  <i class="pi pi-info-circle" />
                  <p>{{ $t('account.cookies_info') }}</p>
                </div>
              </div>

              <template v-if="isSignedIn && nudge.hasEmailAccount.value">
                <div class="settings-field">
                  <label class="settings-label">
                    <i class="pi pi-envelope" />
                    {{ $t('account.signed_in_as') }}
                  </label>
                  <span class="settings-email-display">{{ settingsEmail }}</span>
                </div>
                <button
                  class="nudge-cta sign-out-btn"
                  @click="handleSignOut"
                >
                  <i class="pi pi-sign-out" />
                  {{ $t('account.sign_out') }}
                </button>
              </template>

              <template v-else-if="isConvexConfigured">
                <form
                  class="settings-signup-form"
                  @submit.prevent="handleAccountAuth('signIn')"
                >
                  <input
                    v-model="signupEmail"
                    type="email"
                    class="settings-input"
                    :placeholder="$t('account.email_placeholder')"
                    required
                  />
                  <input
                    v-model="signupPassword"
                    type="password"
                    class="settings-input"
                    :placeholder="$t('account.password_placeholder')"
                    minlength="8"
                    required
                  />
                  <div
                    v-if="signupError"
                    class="signup-error-card"
                  >
                    <p class="nudge-error">{{ displayedSignupError }}</p>
                    <div class="signup-error-actions">
                      <button
                        type="button"
                        class="signup-error-btn"
                        @click="copySignupError"
                      >
                        <i
                          class="pi"
                          :class="signupErrorCopied ? 'pi-check' : 'pi-copy'"
                        />
                        {{ signupErrorCopied ? $t('common.copied') : $t('common.copy') }}
                      </button>
                      <button
                        v-if="signupErrorNeedsCollapse"
                        type="button"
                        class="signup-error-btn"
                        @click="signupErrorExpanded = !signupErrorExpanded"
                      >
                        {{ signupErrorExpanded ? $t('common.show_less') : $t('common.show_more') }}
                      </button>
                    </div>
                  </div>
                  <div class="settings-auth-actions">
                    <button
                      type="submit"
                      class="nudge-cta"
                      :disabled="signupLoading"
                    >
                      <i
                        v-if="signupLoading && authAction === 'signIn'"
                        class="pi pi-spin pi-spinner"
                      />
                      {{ signupLoading && authAction === 'signIn' ? '' : $t('account.sign_in_button') }}
                    </button>
                    <button
                      type="button"
                      class="nudge-cta secondary-auth-btn"
                      :disabled="signupLoading"
                      @click="handleAccountAuth('signUp')"
                    >
                      <i
                        v-if="signupLoading && authAction === 'signUp'"
                        class="pi pi-spin pi-spinner"
                      />
                      {{ signupLoading && authAction === 'signUp' ? '' : $t('account.create_button') }}
                    </button>
                  </div>
                </form>
              </template>

              <div class="settings-backup-section">
                <p class="settings-backup-hint">{{ $t('backup.inline_hint') }}</p>
                <BackupRestore :show-info="false" />
              </div>
            </div>

            <p class="settings-section-label">{{ $t('billing.section_title') }}</p>
            <BillingAccessPanel />

            <p class="settings-section-label">Support</p>
            <div class="settings-account-card">
              <p class="settings-account-hint">
                Diagnostic de cette installation.
              </p>
              <div class="settings-account-actions-row">
                <span class="settings-account-status">
                  {{ buildIdentityLabel }}
                </span>
                <button
                  type="button"
                  class="settings-sync-toggle"
                  @click="copyDiagnostics"
                >
                  <i
                    class="pi"
                    :class="diagnosticsCopied ? 'pi-check' : 'pi-copy'"
                  />
                  <span>{{ diagnosticsCopied ? $t('common.copied') : $t('common.copy') }}</span>
                </button>
              </div>
            </div>

            <!-- Preferences section -->
            <p class="settings-section-label">{{ $t('settings.preferences') }}</p>

            <div class="settings-toggle-row">
              <span class="settings-toggle-label">
                <i class="pi pi-moon" />
                {{ $t('theme.mode_label') }}
              </span>
            </div>
            <div class="settings-theme-mode-group">
              <button
                v-for="mode in themeModes"
                :key="mode.value"
                type="button"
                class="settings-theme-mode-btn"
                :class="{ active: themeStore.themeMode === mode.value }"
                @click="setThemeMode(mode.value)"
              >
                {{ $t(mode.labelKey) }}
              </button>
            </div>
            <p
              v-if="themeStore.themeMode === 'auto'"
              class="settings-theme-hint"
            >
              {{ autoThemeHint }}
            </p>

            <div class="settings-toggle-row">
              <span class="settings-toggle-label">
                <i class="pi pi-palette" />
                {{ $t('theme.focus_mode') }}
              </span>
              <button
                class="friends-toggle-pill"
                :class="{ enabled: themeStore.grayscaleEnabled }"
                @click="themeStore.setGrayscale(!themeStore.grayscaleEnabled)"
              >
                <span class="toggle-thumb" />
              </button>
            </div>

            <div class="settings-toggle-row">
              <span class="settings-toggle-label">
                <i class="pi pi-mobile" />
                {{ $t('settings.haptic_feedback') }}
              </span>
              <button
                class="friends-toggle-pill"
                :class="{ enabled: hapticEnabled }"
                @click="toggleHaptic"
              >
                <span class="toggle-thumb" />
              </button>
            </div>

            <div class="settings-toggle-row">
              <span class="settings-toggle-label">
                <i class="pi pi-volume-up" />
                {{ $t('settings.tap_sound') }}
              </span>
              <button
                class="friends-toggle-pill"
                :class="{ enabled: tapSoundEnabled }"
                @click="toggleTapSound"
              >
                <span class="toggle-thumb" />
              </button>
            </div>
            <div class="settings-sound-variant-row">
              <span class="settings-label settings-sound-variant-label">
                <i class="pi pi-sliders-h" />
                {{ $t('settings.tap_sound_variant') }}
              </span>
              <div class="settings-sound-variant-options">
                <button
                  v-for="option in TAP_SOUND_VARIANTS"
                  :key="option.value"
                  type="button"
                  class="settings-sound-variant-btn"
                  :class="{ active: tapSoundVariant === option.value, disabled: !tapSoundEnabled }"
                  :disabled="!tapSoundEnabled"
                  @click="selectTapSoundVariant(option.value)"
                >
                  {{ $t(option.labelKey) }}
                </button>
              </div>
            </div>

            <!-- Text zoom -->
            <div class="settings-toggle-row">
              <span class="settings-toggle-label">
                <i class="pi pi-search-plus" />
                {{ $t('settings.text_zoom') }}
              </span>
              <span class="text-zoom-value">{{ textZoomLevel }}%</span>
            </div>
            <input
              v-model.number="textZoomLevel"
              type="range"
              class="text-zoom-slider"
              :min="TEXT_ZOOM_MIN"
              :max="TEXT_ZOOM_MAX"
              :step="TEXT_ZOOM_STEP"
              @change="onTextZoomChange"
            />

            <!-- Replay onboarding -->
            <button
              class="settings-replay-btn"
              @click="replayOnboarding"
            >
              <i class="pi pi-info-circle" />
              {{ $t('onboarding.replay_button') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '@/stores/theme'
import { useOnboardingStore } from '@/stores/onboarding'
import { useSignupNudge } from '@/composables/useSignupNudge'
import { signIn, signOut as convexSignOut, isAuthenticated, isConvexConfigured } from '@/lib/convexAuth'
import { finalizePasswordSignIn, resetCloudSyncState, resetSyncedLocalState } from '@/lib/cloudSync'
import { syncSettingsPatch } from '@/lib/cloudSettings'
import { beginPostAuthSyncFeedback, resetPostAuthSyncFeedback } from '@/lib/postAuthSyncFeedback'
import { buildDiagnosticsReport, buildIdentityHeader } from '@/lib/buildDiagnostics'
import { getConvexClient } from '@/lib/convex'
import { api } from '../../../../../../convex/_generated/api'
import { useToast } from 'primevue/usetoast'
import {
  DEFAULT_TAP_SOUND_VARIANT,
  TAP_SOUND_STORAGE_KEY,
  TAP_SOUND_VARIANTS,
  type TapSoundVariant,
  normalizeTapSoundVariant,
} from '../utils/tapSound'
import {
  TEXT_ZOOM_DEFAULT,
  TEXT_ZOOM_MAX,
  TEXT_ZOOM_MIN,
  TEXT_ZOOM_STEP,
  normalizeTextZoomLevel,
} from '../utils/textZoom'
import BackupRestore from './BackupRestore.vue'
import BillingAccessPanel from './BillingAccessPanel.vue'
import type { ThemeMode } from '@/utils/themeAuto'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const { t } = useI18n()
const themeStore = useThemeStore()
const onboardingStore = useOnboardingStore()
const toast = useToast()
const nudge = useSignupNudge()
const isSignedIn = isAuthenticated
const themeModes: Array<{ value: ThemeMode; labelKey: string }> = [
  { value: 'light', labelKey: 'theme.light' },
  { value: 'dark', labelKey: 'theme.dark' },
  { value: 'auto', labelKey: 'theme.auto' },
]
const autoThemeHint = computed(() => {
  const sourceKey = themeStore.autoThemeSource === 'sun'
    ? 'theme.auto_source_sun'
    : 'theme.auto_source_system'
  return `${t('theme.auto_helper')} ${t(sourceKey)}`
})

function setThemeMode(mode: ThemeMode) {
  void themeStore.setThemeMode(mode, { allowPrompt: mode === 'auto' })
}

// ─── Settings state ──────────────────────────────────────────
const settingsEmail = ref(localStorage.getItem('sfz_email') ?? '')

// ─── Signup form ─────────────────────────────────────────────
const signupEmail = ref('')
const signupPassword = ref('')
const authAction = ref<'signIn' | 'signUp'>('signIn')
const signupError = ref('')
const signupLoading = ref(false)
const syncInfoExpanded = ref(false)
const signupErrorCopied = ref(false)
const diagnosticsCopied = ref(false)
const signupErrorExpanded = ref(false)
const SIGNUP_ERROR_PREVIEW_LENGTH = 180
const buildIdentityLabel = computed(() => buildIdentityHeader()[0].replace('commit/build: ', ''))

const signupErrorNeedsCollapse = computed(() =>
  signupError.value.length > SIGNUP_ERROR_PREVIEW_LENGTH || signupError.value.includes('\n'),
)

const displayedSignupError = computed(() => {
  if (signupErrorExpanded.value || !signupErrorNeedsCollapse.value) return signupError.value
  return `${signupError.value.slice(0, SIGNUP_ERROR_PREVIEW_LENGTH).trimEnd()}…`
})

function getAuthErrorMessage(error: unknown, flow: 'signIn' | 'signUp') {
  const message = error instanceof Error ? error.message : ''

  if (flow === 'signUp' && /already exists/i.test(message)) {
    return t('account.already_exists_error')
  }
  if (flow === 'signIn' && /invalid/i.test(message)) {
    return t('account.invalid_credentials_error')
  }

  return message || t('account.error_generic')
}

async function handleAccountAuth(flow: 'signIn' | 'signUp') {
  signupError.value = ''
  signupErrorCopied.value = false
  signupErrorExpanded.value = false
  authAction.value = flow
  signupLoading.value = true
  try {
    const normalizedEmail = signupEmail.value.trim().toLowerCase()
    signupEmail.value = normalizedEmail

    if (flow === 'signUp') {
      const emailExists = await getConvexClient().query(api.users.emailExists, {
        email: normalizedEmail,
      })
      if (emailExists) {
        signupError.value = t('account.already_exists_error')
        return
      }
    }

    beginPostAuthSyncFeedback()
    await signIn('password', {
      email: normalizedEmail,
      password: signupPassword.value,
      flow,
    })
    settingsEmail.value = normalizedEmail
    nudge.onAccountCreated()
    toast.add({
      severity: 'success',
      summary: flow === 'signIn' ? t('account.signed_in_toast') : t('account.created_toast'),
      life: 1800,
    })
    signupPassword.value = ''
    await finalizePasswordSignIn({
      email: normalizedEmail,
      flow,
      reopenSettings: true,
    })
  } catch (e: unknown) {
    resetPostAuthSyncFeedback()
    signupError.value = getAuthErrorMessage(e, flow)
  } finally {
    signupLoading.value = false
  }
}

function closeSheet() {
  emit('update:modelValue', false)
}

const sheetRef = ref<HTMLElement | null>(null)
const dragOffset = ref(0)
const isDragging = ref(false)
const activePointerId = ref<number | null>(null)
const dragStartY = ref(0)
const dragStartTime = ref(0)
let dragResetTimer: number | null = null

const sheetStyle = computed(() => ({
  '--sheet-drag-offset': `${dragOffset.value}px`,
  transition: isDragging.value ? 'none' : 'transform 250ms ease',
}))

function clearDragResetTimer() {
  if (dragResetTimer !== null) {
    window.clearTimeout(dragResetTimer)
    dragResetTimer = null
  }
}

function scheduleDragReset() {
  clearDragResetTimer()
  dragResetTimer = window.setTimeout(() => {
    dragOffset.value = 0
    isDragging.value = false
    activePointerId.value = null
  }, 250)
}

function getDismissThreshold() {
  const sheetHeight = sheetRef.value?.offsetHeight ?? window.innerHeight * 0.5
  return Math.min(140, Math.max(72, sheetHeight * 0.2))
}

function shouldIgnoreDragStart(target: EventTarget | null) {
  if (!(target instanceof Element)) return false
  return Boolean(target.closest('button, a, input, textarea, select, label, [role="button"], [data-no-sheet-drag]'))
}

function onDragStart(event: PointerEvent) {
  if (!props.modelValue || !event.isPrimary) return
  if (event.pointerType === 'mouse' && event.button !== 0) return
  if (shouldIgnoreDragStart(event.target)) return

  isDragging.value = true
  activePointerId.value = event.pointerId
  dragStartY.value = event.clientY
  dragStartTime.value = event.timeStamp || performance.now()
  dragOffset.value = 0
  clearDragResetTimer()

  ;(event.currentTarget as HTMLElement | null)?.setPointerCapture?.(event.pointerId)
}

function onDragMove(event: PointerEvent) {
  if (!isDragging.value || event.pointerId !== activePointerId.value) return

  const nextOffset = Math.max(0, event.clientY - dragStartY.value)
  dragOffset.value = nextOffset

  if (nextOffset > 0) {
    event.preventDefault()
  }
}

function finishDrag(event?: PointerEvent) {
  if (!isDragging.value) return
  if (event && event.pointerId !== activePointerId.value) return

  const elapsed = Math.max(1, (event?.timeStamp || performance.now()) - dragStartTime.value)
  const velocity = dragOffset.value / elapsed
  const shouldClose = dragOffset.value >= getDismissThreshold() || velocity >= 0.6

  isDragging.value = false
  activePointerId.value = null

  if (shouldClose) {
    closeSheet()
    scheduleDragReset()
    return
  }

  dragOffset.value = 0
}

function onDragEnd(event: PointerEvent) {
  finishDrag(event)
}

function onDragCancel(event: PointerEvent) {
  finishDrag(event)
}

async function copySignupError() {
  if (!signupError.value) return

  try {
    await navigator.clipboard.writeText(signupError.value)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = signupError.value
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }

  signupErrorCopied.value = true
  window.setTimeout(() => {
    signupErrorCopied.value = false
  }, 2000)
}

async function copyDiagnostics() {
  const report = buildDiagnosticsReport({
    account_state: isSignedIn.value && nudge.hasEmailAccount.value ? 'signed_in' : 'signed_out',
    convex_configured: isConvexConfigured ? 'yes' : 'no',
    text_zoom: String(textZoomLevel.value),
    haptic_enabled: String(hapticEnabled.value),
    tap_sound_enabled: String(tapSoundEnabled.value),
  })

  try {
    await navigator.clipboard.writeText(report)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = report
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }

  diagnosticsCopied.value = true
  window.setTimeout(() => {
    diagnosticsCopied.value = false
  }, 2000)
}

async function handleSignOut() {
  await convexSignOut()
  resetCloudSyncState()
  resetSyncedLocalState()
  nudge.hasEmailAccount.value = false
  settingsEmail.value = ''
  signupPassword.value = ''
  authAction.value = 'signIn'
  toast.add({ severity: 'success', summary: t('account.signed_out_toast'), life: 3000 })
}

// ─── Haptic & tap sound ─────────────────────────────────────
const hapticEnabled = ref(localStorage.getItem('sfz_haptic') !== 'false')
const tapSoundEnabled = ref(localStorage.getItem('sfz_tap_sound') === 'true')
const tapSoundVariant = ref<TapSoundVariant>(
  normalizeTapSoundVariant(localStorage.getItem(TAP_SOUND_STORAGE_KEY) ?? DEFAULT_TAP_SOUND_VARIANT)
)
const onNativeTapSoundChanged = ((e: CustomEvent) => {
  const enabled = e.detail?.enabled
  if (typeof enabled !== 'boolean') return
  tapSoundEnabled.value = enabled
}) as unknown as (e: Event) => void

if (tapSoundVariant.value !== localStorage.getItem(TAP_SOUND_STORAGE_KEY)) {
  localStorage.setItem(TAP_SOUND_STORAGE_KEY, tapSoundVariant.value)
}

function toggleHaptic() {
  hapticEnabled.value = !hapticEnabled.value
  localStorage.setItem('sfz_haptic', String(hapticEnabled.value))
  syncSettingsPatch({ hapticEnabled: hapticEnabled.value })
  import('@tauri-apps/api/core').then(({ invoke }) => {
    invoke('plugin:android-webview|set_haptic', { enabled: hapticEnabled.value }).catch(() => {})
  }).catch(() => {})
}

function toggleTapSound() {
  tapSoundEnabled.value = !tapSoundEnabled.value
  localStorage.setItem('sfz_tap_sound', String(tapSoundEnabled.value))
  syncSettingsPatch({ tapSoundEnabled: tapSoundEnabled.value })
  import('@tauri-apps/api/core').then(({ invoke }) => {
    invoke('plugin:android-webview|set_tap_sound', { enabled: tapSoundEnabled.value }).catch(() => {})
  }).catch(() => {})
}

function selectTapSoundVariant(variant: TapSoundVariant) {
  tapSoundVariant.value = normalizeTapSoundVariant(variant)
  localStorage.setItem(TAP_SOUND_STORAGE_KEY, tapSoundVariant.value)
  syncSettingsPatch({ tapSoundVariant: tapSoundVariant.value })
  import('@tauri-apps/api/core').then(({ invoke }) => {
    invoke('plugin:android-webview|set_tap_sound_variant', { variant: tapSoundVariant.value }).catch(() => {})
    if (tapSoundEnabled.value) {
      invoke('plugin:android-webview|preview_tap_sound').catch(() => {})
    }
  }).catch(() => {})
}

// ─── Text zoom ──────────────────────────────────────────────
const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
const storedTextZoom = Number(localStorage.getItem('sfz_text_zoom') ?? String(TEXT_ZOOM_DEFAULT))
const textZoomLevel = ref(normalizeTextZoomLevel(storedTextZoom))

if (textZoomLevel.value !== storedTextZoom) {
  localStorage.setItem('sfz_text_zoom', String(textZoomLevel.value))
}

function onTextZoomChange() {
  textZoomLevel.value = normalizeTextZoomLevel(textZoomLevel.value)
  localStorage.setItem('sfz_text_zoom', String(textZoomLevel.value))
  syncSettingsPatch({ textZoom: textZoomLevel.value })
  if (isTauri) {
    import('@tauri-apps/api/core').then(({ invoke }) => {
      invoke('set_text_zoom', { level: textZoomLevel.value }).catch(() => {})
    })
  }
}

function replayOnboarding() {
  emit('update:modelValue', false)
  onboardingStore.reset()
}

watch(() => props.modelValue, (open) => {
  clearDragResetTimer()

  if (open) {
    hapticEnabled.value = localStorage.getItem('sfz_haptic') !== 'false'
    tapSoundEnabled.value = localStorage.getItem('sfz_tap_sound') === 'true'
    tapSoundVariant.value = normalizeTapSoundVariant(
      localStorage.getItem(TAP_SOUND_STORAGE_KEY) ?? DEFAULT_TAP_SOUND_VARIANT
    )
    dragOffset.value = 0
    isDragging.value = false
    activePointerId.value = null
    return
  }

  scheduleDragReset()
})

onMounted(() => {
  window.addEventListener('sfz-tap-sound-changed', onNativeTapSoundChanged)
})

onUnmounted(() => {
  window.removeEventListener('sfz-tap-sound-changed', onNativeTapSoundChanged)
  clearDragResetTimer()
})
</script>

<style scoped>
/* ─── Sheet base (shared with profile sheet) ─────────────────── */

.sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.profile-sheet {
  width: 100%;
  background: var(--surface-card);
  border-radius: 20px 20px 0 0;
  padding-bottom: env(safe-area-inset-bottom, 16px);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transform: translateY(var(--sheet-drag-offset, 0px));
}

.settings-sheet {
  --settings-account-card-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--surface-card) 88%, var(--primary-color) 12%),
    color-mix(in srgb, var(--surface-card) 94%, var(--surface-ground) 6%)
  );
  --settings-account-card-border:
    color-mix(in srgb, var(--surface-border) 68%, var(--primary-color) 32%);
  --settings-account-card-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
  --settings-account-status-bg: rgba(148, 163, 184, 0.18);
  --settings-account-status-color: var(--text-color-secondary);
  --settings-account-status-connected-bg:
    color-mix(in srgb, var(--primary-color) 14%, rgba(255, 255, 255, 0) 86%);
  --settings-account-status-connected-color: var(--primary-color);
  --settings-sync-info-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--surface-ground) 90%, var(--primary-color) 10%),
    color-mix(in srgb, var(--surface-card) 88%, var(--surface-ground) 12%)
  );
  --settings-sync-info-border:
    color-mix(in srgb, var(--surface-border) 80%, var(--primary-color) 20%);
  --settings-sync-warning-icon:
    color-mix(in srgb, var(--primary-color) 55%, #f59e0b 45%);
  --settings-cta-gradient-start:
    color-mix(in srgb, var(--primary-color) 88%, #ffffff 12%);
  --settings-cta-gradient-end:
    color-mix(in srgb, #2563eb 72%, var(--primary-color) 28%);
  --settings-cta-shadow: 0 14px 28px rgba(59, 130, 246, 0.2);
  --settings-secondary-auth-bg:
    color-mix(in srgb, var(--primary-color) 10%, var(--surface-card) 90%);
  --settings-secondary-auth-border:
    color-mix(in srgb, var(--primary-color) 22%, var(--surface-border) 78%);
  --settings-danger-bg: rgba(239, 68, 68, 0.04);
  --settings-danger-border: rgba(239, 68, 68, 0.36);
  --settings-danger-color: #dc2626;
  --settings-error-bg: rgba(239, 68, 68, 0.08);
  --settings-error-border: rgba(239, 68, 68, 0.18);
  --settings-error-text: #dc2626;
  --settings-error-btn-bg:
    color-mix(in srgb, var(--surface-card) 92%, rgba(255, 255, 255, 0.08) 8%);
  --settings-error-btn-border: rgba(239, 68, 68, 0.2);
  --settings-error-btn-text: #b91c1c;
  --settings-backup-divider: rgba(148, 163, 184, 0.18);
}

.settings-sheet.is-dark,
:global(html.dark) .settings-sheet,
:global(.dark) .settings-sheet {
  --settings-account-card-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--surface-card) 92%, var(--primary-color) 8%),
    color-mix(in srgb, var(--surface-card) 96%, #020617 4%)
  );
  --settings-account-card-border:
    color-mix(in srgb, var(--surface-border) 76%, var(--primary-color) 24%);
  --settings-account-card-shadow: 0 24px 48px rgba(2, 6, 23, 0.38);
  --settings-account-status-bg: rgba(255, 255, 255, 0.06);
  --settings-account-status-color:
    color-mix(in srgb, var(--text-color-secondary) 88%, white 12%);
  --settings-account-status-connected-bg:
    color-mix(in srgb, var(--primary-color) 20%, rgba(30, 41, 59, 0.6) 80%);
  --settings-account-status-connected-color: #93c5fd;
  --settings-sync-info-bg: linear-gradient(
    180deg,
    color-mix(in srgb, var(--surface-ground) 64%, var(--surface-card) 36%),
    color-mix(in srgb, var(--surface-card) 90%, #020617 10%)
  );
  --settings-sync-info-border:
    color-mix(in srgb, var(--surface-border) 78%, var(--primary-color) 22%);
  --settings-sync-warning-icon:
    color-mix(in srgb, #f59e0b 74%, var(--primary-color) 26%);
  --settings-cta-gradient-start:
    color-mix(in srgb, var(--primary-color) 92%, white 8%);
  --settings-cta-gradient-end:
    color-mix(in srgb, #0ea5e9 72%, var(--primary-color) 28%);
  --settings-cta-shadow: 0 18px 32px rgba(2, 6, 23, 0.42);
  --settings-secondary-auth-bg:
    color-mix(in srgb, var(--surface-card) 86%, var(--primary-color) 14%);
  --settings-secondary-auth-border:
    color-mix(in srgb, var(--surface-border) 72%, var(--primary-color) 28%);
  --settings-danger-bg: rgba(239, 68, 68, 0.1);
  --settings-danger-border: rgba(248, 113, 113, 0.32);
  --settings-danger-color: #fca5a5;
  --settings-error-bg: rgba(127, 29, 29, 0.26);
  --settings-error-border: rgba(248, 113, 113, 0.22);
  --settings-error-text: #fecaca;
  --settings-error-btn-bg:
    color-mix(in srgb, var(--surface-card) 86%, rgba(248, 113, 113, 0.12) 14%);
  --settings-error-btn-border: rgba(248, 113, 113, 0.22);
  --settings-error-btn-text: #fca5a5;
  --settings-backup-divider: rgba(161, 161, 170, 0.16);
}

.sheet-handle {
  width: 2.5rem;
  height: 4px;
  background: var(--surface-border);
  border-radius: 2px;
  margin: 0.75rem auto 0;
  flex-shrink: 0;
}

.sheet-drag-zone {
  flex-shrink: 0;
  touch-action: none;
  user-select: none;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem 0.5rem;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-color);
}

.sheet-close-btn {
  background: var(--surface-ground);
  border: none;
  border-radius: 50%;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-color-secondary);
  font-size: 0.75rem;
  transition: background-color 0.12s;
}

.sheet-close-btn:active {
  background: var(--surface-hover);
}

.sheet-close-btn:disabled {
  opacity: 0.45;
  cursor: default;
}

/* ─── Settings content ───────────────────────────────────────── */

.settings-content {
  padding: 0 1.25rem 1.5rem;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--primary-color, #6366f1) transparent;
  scrollbar-gutter: stable;
}

.settings-content::-webkit-scrollbar {
  width: 6px;
  -webkit-appearance: none;
}

.settings-content::-webkit-scrollbar-track {
  background: transparent;
}

.settings-content::-webkit-scrollbar-thumb {
  background: var(--primary-color, #6366f1);
  border-radius: 3px;
  opacity: 0.6;
}

.settings-section-label {
  margin: 1rem 0 0.5rem;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-color-secondary);
}

.settings-sound-variant-row {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  margin: -0.2rem 0 0.8rem;
}

.settings-sound-variant-label {
  margin-bottom: 0;
}

.settings-sound-variant-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
}

.settings-sound-variant-btn {
  min-height: 2.4rem;
  padding: 0.55rem 0.6rem;
  border-radius: 12px;
  border: 1px solid var(--surface-border);
  background: var(--surface-ground);
  color: var(--text-color);
  font-size: 0.8rem;
  font-weight: 600;
  transition: border-color 0.15s, background-color 0.15s, color 0.15s, opacity 0.15s;
}

.settings-sound-variant-btn.active {
  background: color-mix(in srgb, var(--primary-color) 12%, var(--surface-card) 88%);
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--surface-border) 58%);
  color: var(--primary-color);
}

.settings-sound-variant-btn.disabled {
  opacity: 0.45;
}

.settings-field {
  margin-bottom: 0.75rem;
}

.settings-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-color-secondary);
  margin-bottom: 0.35rem;
}

.settings-label i {
  font-size: 0.85rem;
}

.settings-input {
  width: 100%;
  padding: 0.6rem 0.75rem;
  font-size: 0.9rem;
  background: var(--surface-ground);
  border: 1px solid var(--surface-border);
  border-radius: 10px;
  color: var(--text-color);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.settings-input:focus {
  border-color: var(--primary-color);
}

.settings-account-hint {
  font-size: 0.82rem;
  color: var(--text-color-secondary);
  line-height: 1.45;
  margin: 0;
}

.settings-account-card {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 0.9rem;
  margin-bottom: 1rem;
  border-radius: 16px;
  background: var(--settings-account-card-bg);
  border: 1px solid var(--settings-account-card-border);
  box-shadow: var(--settings-account-card-shadow);
}

.settings-account-card-header {
  display: block;
}

.settings-account-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.3rem 0.55rem;
  border-radius: 999px;
  background: var(--settings-account-status-bg);
  color: var(--settings-account-status-color);
  font-size: 0.72rem;
  font-weight: 700;
  white-space: nowrap;
}

.settings-account-actions-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.settings-account-status.connected {
  background: var(--settings-account-status-connected-bg);
  color: var(--settings-account-status-connected-color);
}

.settings-sync-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--primary-color);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.settings-sync-info-box {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.7rem 0.8rem;
  border-radius: 14px;
  background: var(--settings-sync-info-bg);
  border: 1px solid var(--settings-sync-info-border);
}

.settings-sync-info-row,
.settings-sync-warning-row {
  display: flex;
  align-items: flex-start;
  gap: 0.55rem;
}

.settings-sync-info-row i {
  color: var(--primary-color);
  font-size: 0.95rem;
  margin-top: 0.15rem;
}

.settings-sync-warning-row i {
  color: var(--settings-sync-warning-icon);
  font-size: 0.95rem;
  margin-top: 0.15rem;
}

.settings-sync-info-row p,
.settings-sync-warning-row p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--text-color-secondary);
}

.settings-signup-form {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.settings-auth-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}

.settings-signup-form .nudge-cta,
.sign-out-btn {
  width: 100%;
  padding: 0.7rem;
  border: none;
  border-radius: 10px;
  background: linear-gradient(
    135deg,
    var(--settings-cta-gradient-start),
    var(--settings-cta-gradient-end)
  );
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 0;
  min-height: 2.7rem;
  box-shadow: var(--settings-cta-shadow);
}

.settings-signup-form .nudge-cta:disabled {
  opacity: 0.6;
}

.secondary-auth-btn {
  background: var(--settings-secondary-auth-bg);
  color: var(--primary-color);
  border: 1px solid var(--settings-secondary-auth-border);
  box-shadow: none;
}

.sign-out-btn {
  background: var(--settings-danger-bg);
  color: var(--settings-danger-color);
  border: 1px solid var(--settings-danger-border);
  margin-top: 0;
  box-shadow: none;
}

.signup-error-card {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.7rem 0.8rem;
  border-radius: 12px;
  background: var(--settings-error-bg);
  border: 1px solid var(--settings-error-border);
}

.nudge-error {
  margin: 0;
  color: var(--settings-error-text);
  font-size: 0.8rem;
  line-height: 1.45;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.signup-error-actions {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.signup-error-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.6rem;
  border: 1px solid var(--settings-error-btn-border);
  border-radius: 999px;
  background: var(--settings-error-btn-bg);
  color: var(--settings-error-btn-text);
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
}

.settings-email-display {
  font-size: 0.85rem;
  color: var(--text-color);
  font-weight: 500;
}

.settings-backup-section {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding-top: 0.05rem;
  border-top: 1px solid var(--settings-backup-divider);
}

.settings-backup-hint {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--text-color-secondary);
}

.settings-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0;
  border-bottom: 1px solid var(--surface-border);
}

.settings-toggle-row:last-child {
  border-bottom: none;
}

.settings-theme-mode-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
  margin: -0.15rem 0 0.8rem;
}

.settings-theme-mode-btn {
  min-height: 2.45rem;
  padding: 0.55rem 0.6rem;
  border-radius: 12px;
  border: 1px solid var(--surface-border);
  background: var(--surface-ground);
  color: var(--text-color-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  transition: border-color 0.15s, background-color 0.15s, color 0.15s;
}

.settings-theme-mode-btn.active {
  background: color-mix(in srgb, var(--primary-color) 12%, var(--surface-card) 88%);
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--surface-border) 58%);
  color: var(--primary-color);
}

.settings-theme-hint {
  margin: -0.3rem 0 0.8rem;
  font-size: 0.78rem;
  line-height: 1.4;
  color: var(--text-color-secondary);
}

.settings-toggle-label {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-color);
}

.settings-toggle-label i {
  font-size: 1rem;
  width: 2rem;
  text-align: center;
}

.friends-toggle-pill {
  position: relative;
  width: 2.8rem;
  height: 1.6rem;
  border-radius: 1rem;
  border: none;
  background: var(--surface-border);
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
  padding: 0;
}

.friends-toggle-pill.enabled {
  background: var(--primary-color);
}

.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 1.1rem;
  height: 1.1rem;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: transform 0.2s;
}

.friends-toggle-pill.enabled .toggle-thumb {
  transform: translateX(1.2rem);
}

.text-zoom-value {
  font-size: 0.85rem;
  color: var(--primary-color);
  font-weight: 600;
  min-width: 3rem;
  text-align: right;
}

.text-zoom-slider {
  width: 100%;
  margin: 0 0 0.75rem;
  accent-color: var(--primary-color);
}

.settings-replay-btn {
  width: 100%;
  padding: 0.75rem 1rem;
  margin-top: 0.5rem;
  border-radius: 10px;
  border: 1px solid var(--surface-border);
  background: var(--surface-card);
  color: var(--text-color-secondary);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
}

.settings-replay-btn:active {
  background: var(--surface-hover);
}

/* ─── Sheet transition ───────────────────────────────────────── */

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.25s ease;
}

.sheet-enter-active .profile-sheet,
.sheet-leave-active .profile-sheet {
  transition: transform 0.25s ease;
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .profile-sheet,
.sheet-leave-to .profile-sheet {
  transform: translateY(calc(100% + var(--sheet-drag-offset, 0px)));
}

@media (prefers-reduced-motion: reduce) {
  .sheet-enter-active,
  .sheet-leave-active,
  .sheet-enter-active .profile-sheet,
  .sheet-leave-active .profile-sheet,
  .friends-toggle-pill,
  .toggle-thumb {
    transition: none;
  }
}

@media (max-width: 420px) {
  .settings-account-card {
    gap: 0.75rem;
    padding: 0.8rem;
  }

  .settings-account-actions-row {
    gap: 0.5rem;
  }

  .settings-account-status,
  .settings-sync-toggle {
    font-size: 0.72rem;
  }
}
</style>

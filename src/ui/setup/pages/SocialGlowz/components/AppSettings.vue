<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="$t('common.settings')"
    :style="{ width: '50vw' }"
    :breakpoints="{ '960px': '75vw', '641px': '90vw' }"
  >
    <div class="settings-container">
      <!-- Language -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-globe mr-2"></i>
          <span>{{ $t('settings.language') }}</span>
        </div>
        <select
          v-model="currentLocale"
          class="locale-select"
          @change="onLocaleChange"
        >
          <option value="fr">Français</option>
          <option value="en">English</option>
        </select>
      </div>

      <Divider />

      <!-- Theme Toggle -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-moon mr-2"></i>
          <span>{{ $t('theme.mode_label') }}</span>
        </div>
        <div class="theme-mode-group">
          <button
            v-for="mode in themeModes"
            :key="mode.value"
            type="button"
            class="theme-mode-btn"
            :class="{ active: themeStore.themeMode === mode.value }"
            @click="setThemeMode(mode.value)"
          >
            {{ $t(mode.labelKey) }}
          </button>
        </div>
      </div>

      <div
        v-if="themeStore.themeMode === 'auto'"
        class="theme-mode-hint"
      >
        {{ autoThemeHint }}
      </div>

      <Divider />

      <!-- Grayscale / focus mode -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-palette mr-2"></i>
          <span>{{ $t('theme.focus_mode') }}</span>
        </div>
        <InputSwitch
          :model-value="themeStore.grayscaleEnabled"
          @change="themeStore.setGrayscale(!themeStore.grayscaleEnabled)"
        />
      </div>

      <Divider />

      <!-- Other settings -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-bell mr-2"></i>
          <span>{{ $t('common.notifications') }}</span>
        </div>
        <InputSwitch v-model="notifications" />
      </div>

      <Divider />

      <!-- Replay onboarding -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-info-circle mr-2"></i>
          <span>{{ $t('onboarding.replay_button') }}</span>
        </div>
        <button
          class="replay-btn"
          @click="replayOnboarding"
        >
          <i class="pi pi-refresh" />
        </button>
      </div>

      <Divider />

      <!-- Text zoom -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-search-plus mr-2"></i>
          <span>{{ $t('settings.text_zoom') }}</span>
        </div>
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

      <Divider />

      <BillingAccessPanel />

      <Divider />

      <!-- Backup / Restore -->
      <div class="setting-item">
        <div class="setting-label">
          <i class="pi pi-database mr-2"></i>
          <span>{{ $t('backup.section_title') }}</span>
        </div>
      </div>
      <BackupRestore />
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/utils/i18n'
import { syncSettingsPatch } from '@/lib/cloudSettings'
import {
  TEXT_ZOOM_DEFAULT,
  TEXT_ZOOM_MAX,
  TEXT_ZOOM_MIN,
  TEXT_ZOOM_STEP,
  normalizeTextZoomLevel,
} from '../utils/textZoom'
import { useThemeStore } from '@/stores/theme'
import { useOnboardingStore } from '@/stores/onboarding'
import Dialog from 'primevue/dialog'
import Divider from 'primevue/divider'
import BackupRestore from './BackupRestore.vue'
import BillingAccessPanel from './BillingAccessPanel.vue'
import type { ThemeMode } from '@/utils/themeAuto'

const { locale, t } = useI18n()

const visible = ref(false)
const notifications = ref(true)
const currentLocale = ref(locale.value)

const themeStore = useThemeStore()
const onboardingStore = useOnboardingStore()
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

function onLocaleChange() {
  setLocale(currentLocale.value)
}

function replayOnboarding() {
  visible.value = false
  onboardingStore.reset()
}

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

defineExpose({
  show: () => visible.value = true
})
</script>

<style scoped>
.settings-container {
  padding: 1rem;
  max-height: 70vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--primary-color, #6366f1) transparent;
  scrollbar-gutter: stable;
}

.settings-container::-webkit-scrollbar {
  width: 6px;
  -webkit-appearance: none;
}

.settings-container::-webkit-scrollbar-track {
  background: transparent;
}

.settings-container::-webkit-scrollbar-thumb {
  background: var(--primary-color, #6366f1);
  border-radius: 3px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.setting-label {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.setting-label i {
  margin-right: 0.5rem;
}

.locale-select {
  padding: 0.35rem 0.6rem;
  border-radius: 8px;
  border: 1px solid var(--surface-border, #ddd);
  background: var(--surface-card, #fff);
  color: var(--text-color, #333);
  font-size: 0.85rem;
  cursor: pointer;
}

.theme-mode-group {
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.2rem;
  border-radius: 12px;
  border: 1px solid var(--surface-border, #ddd);
  background: var(--surface-ground, #f6f6f6);
}

.theme-mode-btn {
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--text-color-secondary, #666);
  padding: 0.42rem 0.7rem;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.theme-mode-btn.active {
  background: var(--surface-card, #fff);
  color: var(--primary-color, #6366f1);
}

.theme-mode-hint {
  margin: -0.45rem 0 0.9rem;
  font-size: 0.78rem;
  line-height: 1.4;
  color: var(--text-color-secondary, #666);
}

.text-zoom-value {
  font-size: 0.85rem;
  color: var(--primary-color);
  font-weight: 600;
}

.text-zoom-slider {
  width: 100%;
  margin: -0.5rem 0 0.5rem;
  accent-color: var(--primary-color);
}

.replay-btn {
  padding: 0.4rem 0.7rem;
  border-radius: 8px;
  border: 1px solid var(--surface-border);
  background: var(--surface-card);
  color: var(--text-color-secondary);
  cursor: pointer;
  transition: background 0.15s;
}

.replay-btn:hover {
  background: var(--surface-hover);
}

:global(.dark) .locale-select {
  background: #313244;
  border-color: #45475a;
  color: #cdd6f4;
}

:global(.dark) .theme-mode-group {
  background: #1f2432;
  border-color: #45475a;
}

:global(.dark) .theme-mode-btn.active {
  background: #313244;
  color: #89b4fa;
}
</style>

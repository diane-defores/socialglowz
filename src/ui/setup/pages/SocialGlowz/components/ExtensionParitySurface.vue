<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { builtInSocialNetworks } from "@/config/socialNetworks"
import { i18n, setLocale } from "@/utils/i18n"
import { useProfilesStore } from "@/stores/profiles"
import { useCustomLinksStore } from "@/stores/customLinks"
import { useThemeStore } from "@/stores/theme"
import { getPlatformCapabilities } from "@/platform/capabilities"
import {
  launchExternalUrl,
  normalizeHttpsUrl,
  openExtensionDashboard,
  openExtensionSidePanel,
  type ExtensionLaunchErrorCode,
} from "@/platform/extensionNetworkLauncher"

type ExtensionSurface = "popup" | "side-panel" | "options" | "install" | "update" | "setup"

const props = withDefaults(
  defineProps<{
    surface: ExtensionSurface
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)

const profilesStore = useProfilesStore()
const customLinksStore = useCustomLinksStore()
const themeStore = useThemeStore()
const capabilities = getPlatformCapabilities()

const customLabel = ref("")
const customUrl = ref("")
const statusMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)

const locale = computed({
  get: () => i18n.global.locale.value,
  set: (nextLocale: string) => {
    setLocale(nextLocale, false)
  },
})

const headerKey = computed(() => {
  const keyBySurface: Record<ExtensionSurface, string> = {
    popup: "extension.surface.popup_title",
    "side-panel": "extension.surface.side_panel_title",
    options: "extension.surface.options_title",
    install: "extension.surface.install_title",
    update: "extension.surface.update_title",
    setup: "extension.surface.setup_title",
  }
  return keyBySurface[props.surface]
})

const descriptionKey = computed(() => {
  const keyBySurface: Record<ExtensionSurface, string> = {
    popup: "extension.surface.popup_description",
    "side-panel": "extension.surface.side_panel_description",
    options: "extension.surface.options_description",
    install: "extension.surface.install_description",
    update: "extension.surface.update_description",
    setup: "extension.surface.setup_description",
  }
  return keyBySurface[props.surface]
})

const activeProfileId = computed({
  get: () => profilesStore.activeProfileId,
  set: (profileId: string) => {
    if (!profileId || profileId === profilesStore.activeProfileId) return
    profilesStore.setActive(profileId)
  },
})

const activeProfile = computed(() => profilesStore.activeProfile)

const visibleNetworks = computed(() => {
  const hiddenIds = new Set(activeProfile.value?.hiddenNetworks ?? [])
  const allNetworks = builtInSocialNetworks.filter((network) => !hiddenIds.has(network.id))
  if (props.compact) {
    return allNetworks.slice(0, 8)
  }
  return allNetworks
})

const profileLinks = computed(() => {
  if (!activeProfileId.value) return []
  return customLinksStore.getLinks(activeProfileId.value)
})

const canOpenSidePanel = computed(() => capabilities.supportsSidePanel)
const isDarkMode = computed(() => themeStore.isDarkMode)

function messageForCode(code: ExtensionLaunchErrorCode): string {
  return i18n.global.t(`extension.launch.errors.${code}`)
}

function clearMessages() {
  statusMessage.value = null
  errorMessage.value = null
}

async function openBuiltInNetwork(url: string) {
  clearMessages()
  const result = await launchExternalUrl(url)
  if (!result.ok) {
    errorMessage.value = messageForCode(result.code)
    return
  }
  statusMessage.value = i18n.global.t("extension.launch.opened")
}

async function openCustomLink(url: string) {
  clearMessages()
  const result = await launchExternalUrl(url)
  if (!result.ok) {
    errorMessage.value = messageForCode(result.code)
    return
  }
  statusMessage.value = i18n.global.t("extension.launch.opened")
}

async function addCustomLink() {
  clearMessages()
  if (!activeProfileId.value) {
    errorMessage.value = i18n.global.t("extension.launch.errors.invalid")
    return
  }

  const validatedUrl = normalizeHttpsUrl(customUrl.value)
  if (!validatedUrl.ok) {
    errorMessage.value = messageForCode(validatedUrl.code)
    return
  }

  const label = customLabel.value.trim()
  if (!label) {
    errorMessage.value = i18n.global.t("extension.launch.errors.empty")
    return
  }

  customLinksStore.addLink(activeProfileId.value, label, validatedUrl.url)
  customLabel.value = ""
  customUrl.value = ""
  statusMessage.value = i18n.global.t("extension.launch.custom_link_added")
}

async function openDashboard() {
  clearMessages()
  const result = await openExtensionDashboard()
  if (!result.ok) {
    errorMessage.value = messageForCode(result.code)
    return
  }
  statusMessage.value = i18n.global.t("extension.launch.dashboard_opened")
}

async function openSidePanel() {
  clearMessages()
  const result = await openExtensionSidePanel()
  if (!result.ok) {
    errorMessage.value = messageForCode(result.code)
    return
  }
  statusMessage.value = i18n.global.t("extension.launch.side_panel_opened")
}

function toggleTheme() {
  const nextMode = isDarkMode.value ? "light" : "dark"
  void themeStore.setThemeMode(nextMode, { allowPrompt: false })
}

onMounted(() => {
  themeStore.initTheme()
  const ensured = profilesStore.ensureDefault()
  if (!profilesStore.activeProfileId) {
    profilesStore.setActive(ensured.id)
  }
})
</script>

<template>
  <section class="mx-auto w-full max-w-5xl p-4 space-y-4">
    <header class="space-y-1">
      <h1 class="text-2xl font-semibold">
        {{ $t(headerKey) }}
      </h1>
      <p class="text-sm opacity-80">
        {{ $t(descriptionKey) }}
      </p>
    </header>

    <div class="grid gap-3 md:grid-cols-3">
      <label class="form-control">
        <span class="label-text font-semibold">{{ $t("extension.profile.label") }}</span>
        <select v-model="activeProfileId">
          <option
            v-for="profile in profilesStore.profiles"
            :key="profile.id"
            :value="profile.id"
          >
            {{ profile.emoji }} {{ profile.name }}
          </option>
        </select>
      </label>

      <label class="form-control">
        <span class="label-text font-semibold">{{ $t("settings.language") }}</span>
        <select v-model="locale">
          <option value="fr">Français</option>
          <option value="en">English</option>
        </select>
      </label>

      <div class="flex items-end">
        <button
          class="btn btn-outline w-full"
          type="button"
          @click="toggleTheme"
        >
          {{ isDarkMode ? $t("theme.light") : $t("theme.dark") }}
        </button>
      </div>
    </div>

    <div class="space-y-2">
      <h2 class="text-lg font-semibold">
        {{ $t("extension.networks.title") }}
      </h2>
      <div
        class="grid gap-2"
        :class="props.compact ? 'grid-cols-2' : 'grid-cols-2 md:grid-cols-4 lg:grid-cols-5'"
      >
        <button
          v-for="network in visibleNetworks"
          :key="network.id"
          class="btn btn-sm btn-primary justify-start"
          type="button"
          @click="openBuiltInNetwork(network.url)"
        >
          {{ network.label }}
        </button>
      </div>
    </div>

    <div class="space-y-2">
      <h2 class="text-lg font-semibold">
        {{ $t("extension.custom_links.title") }}
      </h2>
      <div class="grid gap-2 md:grid-cols-[1fr,1fr,auto]">
        <input
          v-model="customLabel"
          type="text"
          :placeholder="$t('extension.custom_links.name_placeholder')"
        />
        <input
          v-model="customUrl"
          type="text"
          :placeholder="$t('extension.custom_links.url_placeholder')"
        />
        <button
          class="btn btn-secondary"
          type="button"
          @click="addCustomLink"
        >
          {{ $t("common.add") }}
        </button>
      </div>

      <ul class="space-y-2">
        <li
          v-for="link in profileLinks"
          :key="link.id"
          class="flex items-center justify-between gap-2 rounded border border-base-300 p-2"
        >
          <span class="truncate">{{ link.label }}</span>
          <button
            class="btn btn-xs btn-outline"
            type="button"
            @click="openCustomLink(link.url)"
          >
            {{ $t("common.open") }}
          </button>
        </li>
      </ul>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        class="btn btn-outline"
        type="button"
        @click="openDashboard"
      >
        {{ $t("extension.actions.open_dashboard") }}
      </button>
      <button
        class="btn btn-outline"
        type="button"
        :disabled="!canOpenSidePanel"
        @click="openSidePanel"
      >
        {{ $t("extension.actions.open_side_panel") }}
      </button>
    </div>

    <div
      v-if="statusMessage"
      class="alert alert-success text-sm"
    >
      {{ statusMessage }}
    </div>
    <div
      v-if="errorMessage"
      class="alert alert-error text-sm"
    >
      {{ errorMessage }}
    </div>

    <div class="rounded border border-warning/40 bg-warning/5 p-3 text-sm space-y-1">
      <p>{{ $t("extension.limitations.session_isolation") }}</p>
      <p>{{ $t("extension.limitations.native_backup") }}</p>
      <p>{{ $t("extension.limitations.native_haptics") }}</p>
    </div>
  </section>
</template>

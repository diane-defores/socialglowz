<template>
  <!-- Transparent host div — the native Tauri webview floats on top -->
  <div
    ref="hostEl"
    class="webview-host"
  >
    <!-- Dev-mode placeholder (running in browser, not Tauri) -->
    <div
      v-if="!isTauri"
      class="dev-placeholder"
    >
      <div class="placeholder-content">
        <i
          class="pi pi-desktop"
          style="font-size: 3rem; opacity: 0.3"
        />
        <p><strong>{{ webviewStore.activeNetworkId }}</strong></p>
        <p>{{ profilesStore.activeProfile?.emoji }} {{ profilesStore.activeProfile?.name ?? 'No profile' }}</p>
        <p class="hint">Native webview renders here in the Tauri desktop app.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useWebviewStore, WEBVIEW_URLS } from '@/stores/webviewState'
import { useProfilesStore } from '@/stores/profiles'
import { getNetworkIsolationOriginsByNetwork } from '@/config/socialNetworks'
import { useNetworkWebview } from '../composables/useNetworkWebview'

const webviewStore = useWebviewStore()
const profilesStore = useProfilesStore()
const hostEl = ref<HTMLElement | null>(null)
const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window

const { open, switchTo, close } = useNetworkWebview(hostEl)

// Kotlin bottom bar events are handled in App.vue via CustomEvents (evaluateJavascript).
// Network switching is handled entirely in Kotlin (direct loadUrl) — no Vue IPC needed.
// Back/close sends 'sfz-webview-back' CustomEvent → App.vue calls clearNetwork().

const activeUrl = computed(() => webviewStore.activeUrl)
const activeNetworkId = computed(() => webviewStore.activeNetworkId)
const activeProfileId = computed(() => profilesStore.activeProfileId)

/** Send the list of visible webview network IDs to the Android bottom bar. */
async function syncBarNetworks() {
  if (!isTauri) return
  const profileId = profilesStore.activeProfileId
  if (!profileId) return
  const allWebviewIds = Object.keys(WEBVIEW_URLS)
  const visibleIds = allWebviewIds.filter(id => !profilesStore.isNetworkHidden(profileId, id))
  try {
    const { invoke } = await import('@tauri-apps/api/core')
    await invoke('set_bar_networks', {
      networkIds: visibleIds,
      storageOriginsByNetwork: getNetworkIsolationOriginsByNetwork(visibleIds),
    })
  } catch { /* no-op on desktop */ }
}

// React to network or profile changes — open or switch the webview
watch(
  [activeUrl, activeNetworkId, activeProfileId],
  async ([url, networkId, profileId], [prevUrl, prevNetworkId, prevProfileId]) => {
    if (!url || !networkId || !profileId) {
      await close()
      return
    }
    const keyChanged =
      networkId !== prevNetworkId || profileId !== prevProfileId || url !== prevUrl
    if (keyChanged && (prevNetworkId || prevProfileId)) {
      await switchTo(url, profileId, networkId)
    } else if (!prevNetworkId && !prevProfileId) {
      await open(url, profileId, networkId)
    }
  },
  { immediate: true },
)

// The watch({ immediate: true }) above handles the initial open on mount.
// No separate onMounted needed — it would cause a redundant double open_webview IPC.
</script>

<style scoped>
.webview-host {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: transparent;
  position: relative;
}

.dev-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-color-secondary);
}

.placeholder-content {
  text-align: center;
  padding: 2rem;
}

.placeholder-content p {
  margin: 0.5rem 0;
}

.hint {
  font-size: 0.85rem;
  opacity: 0.6;
}
</style>

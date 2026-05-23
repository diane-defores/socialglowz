import { ref, watch, onUnmounted, type Ref } from 'vue'
import { useElementBounding } from '@vueuse/core'
import { getNetworkIsolationOrigins } from '@/config/socialNetworks'

const isTauri = () =>
  typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window

async function invoke(cmd: string, args?: Record<string, unknown>) {
  if (!isTauri()) return
  const { invoke: tauriInvoke } = await import('@tauri-apps/api/core')
  return tauriInvoke(cmd, args)
}

/**
 * Manages a native Tauri child webview for a (profile, network) pair,
 * positioned over the given host element.
 * Each profile×network gets its own isolated data directory
 * → separate cookies / localStorage / IndexedDB.
 */
export function useNetworkWebview(hostEl: Ref<HTMLElement | null>) {
  const { x, y, width, height } = useElementBounding(hostEl)

  // Track what's currently open as "profileId:networkId"
  const activeKey = ref<string | null>(null)
  const isOpen = ref(false)

  // Keep bounds in sync on sidebar toggle / window resize
  watch([x, y, width, height], async ([nx, ny, nw, nh]) => {
    if (isOpen.value && activeKey.value && nw > 0 && nh > 0) {
      const [profileId, networkId] = activeKey.value.split(':')
      await invoke('resize_webview', {
        profileId,
        networkId,
        x: nx,
        y: ny,
        width: nw,
        height: nh,
      })
    }
  })

  async function open(url: string, profileId: string, networkId: string) {
    const storageOrigins = getNetworkIsolationOrigins(networkId)
    await invoke('open_webview', {
      url,
      profileId,
      networkId,
      storageOrigins,
      x: x.value,
      y: y.value,
      width: width.value,
      height: height.value,
    })
    activeKey.value = `${profileId}:${networkId}`
    isOpen.value = true
  }

  /**
   * Switch to a different profile or network — hide the old webview (keep it
   * alive in the pool) and show/create the new one. Preserves page state,
   * scroll position, and cookies across switches.
   */
  async function switchTo(url: string, profileId: string, networkId: string) {
    // Hide the currently visible webview (stays alive off-screen)
    if (isOpen.value && activeKey.value) {
      const [oldProfileId, oldNetworkId] = activeKey.value.split(':')
      await invoke('hide_webview', { profileId: oldProfileId, networkId: oldNetworkId })
    }

    // Try to show an existing pooled webview (instant — no page reload)
    const shown = await invoke('show_webview', {
      profileId,
      networkId,
      x: x.value,
      y: y.value,
      width: width.value,
      height: height.value,
    })

    if (!shown) {
      // First time opening this network — create a fresh webview
      const storageOrigins = getNetworkIsolationOrigins(networkId)
      await invoke('open_webview', {
        url,
        profileId,
        networkId,
        storageOrigins,
        x: x.value,
        y: y.value,
        width: width.value,
        height: height.value,
      })
    }

    activeKey.value = `${profileId}:${networkId}`
    isOpen.value = true
  }

  /** Hide the active webview (pooled — stays alive for instant re-show). */
  async function close() {
    if (isOpen.value && activeKey.value) {
      const [profileId, networkId] = activeKey.value.split(':')
      await invoke('hide_webview', { profileId, networkId })
      activeKey.value = null
      isOpen.value = false
    }
  }

  onUnmounted(close)

  return { open, switchTo, close, isOpen, activeKey }
}

import { supportsSidePanel } from "@/platform/capabilities"

const FORBIDDEN_PROTOCOLS = new Set([
  "javascript:",
  "data:",
  "file:",
  "chrome:",
  "chrome-extension:",
  "moz-extension:",
])

export type UrlValidationErrorCode =
  | "empty"
  | "invalid"
  | "forbidden_protocol"
  | "https_required"
  | "credentials_not_allowed"

export type UrlValidationSuccess = {
  ok: true
  url: string
  host: string
}

export type UrlValidationFailure = {
  ok: false
  code: UrlValidationErrorCode
}

export type UrlValidationResult = UrlValidationSuccess | UrlValidationFailure

export type ExtensionLaunchErrorCode =
  | UrlValidationErrorCode
  | "tabs_api_unavailable"
  | "tab_creation_failed"
  | "runtime_url_unavailable"
  | "side_panel_unavailable"
  | "side_panel_failed"

export type ExtensionLaunchResult =
  | { ok: true }
  | { ok: false; code: ExtensionLaunchErrorCode }

function parseCandidateUrl(rawInput: string): URL | null {
  const trimmed = rawInput.trim()
  if (!trimmed) return null
  const candidate = /^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(trimmed) ? trimmed : `https://${trimmed}`
  try {
    return new URL(candidate)
  } catch {
    return null
  }
}

export function normalizeHttpsUrl(rawInput: string): UrlValidationResult {
  const trimmed = rawInput.trim()
  if (!trimmed) {
    return { ok: false, code: "empty" }
  }

  const parsed = parseCandidateUrl(trimmed)
  if (!parsed) {
    return { ok: false, code: "invalid" }
  }

  if (FORBIDDEN_PROTOCOLS.has(parsed.protocol)) {
    return { ok: false, code: "forbidden_protocol" }
  }

  if (parsed.protocol !== "https:") {
    return { ok: false, code: "https_required" }
  }

  if (parsed.username || parsed.password) {
    return { ok: false, code: "credentials_not_allowed" }
  }

  return {
    ok: true,
    url: parsed.toString(),
    host: parsed.host,
  }
}

function withChromeTabsCreate(url: string): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const chromeApi = globalThis.chrome
    if (!chromeApi?.tabs?.create) {
      reject(new Error("tabs.create unavailable"))
      return
    }
    chromeApi.tabs.create({ url }, () => {
      const runtimeError = chromeApi.runtime?.lastError
      if (runtimeError) {
        reject(new Error(runtimeError.message))
        return
      }
      resolve(true)
    })
  })
}

async function withBrowserTabsCreate(url: string): Promise<boolean> {
  const browserApi = (globalThis as { browser?: { tabs?: { create?: (props: { url: string }) => Promise<unknown> } } }).browser
  await browserApi?.tabs?.create?.({ url })
  return true
}

async function openInNewTab(url: string): Promise<ExtensionLaunchResult> {
  try {
    if (globalThis.chrome?.tabs?.create) {
      await withChromeTabsCreate(url)
      return { ok: true }
    }

    const browserApi = (globalThis as { browser?: { tabs?: { create?: (props: { url: string }) => Promise<unknown> } } }).browser
    if (browserApi?.tabs?.create) {
      await withBrowserTabsCreate(url)
      return { ok: true }
    }

    return { ok: false, code: "tabs_api_unavailable" }
  } catch {
    return { ok: false, code: "tab_creation_failed" }
  }
}

export async function launchExternalUrl(rawInput: string): Promise<ExtensionLaunchResult> {
  const normalized = normalizeHttpsUrl(rawInput)
  if (!normalized.ok) {
    return { ok: false, code: normalized.code }
  }

  return openInNewTab(normalized.url)
}

export async function openExtensionDashboard(route = "/setup/SocialFlow"): Promise<ExtensionLaunchResult> {
  const runtimeUrl = globalThis.chrome?.runtime?.getURL?.(`src/ui/setup/index.html#${route}`)
  if (!runtimeUrl) {
    return { ok: false, code: "runtime_url_unavailable" }
  }
  return openInNewTab(runtimeUrl)
}

function getCurrentWindowId(): Promise<number> {
  return new Promise((resolve, reject) => {
    const chromeApi = globalThis.chrome
    if (!chromeApi?.windows?.getCurrent) {
      reject(new Error("windows.getCurrent unavailable"))
      return
    }
    chromeApi.windows.getCurrent((window) => {
      const runtimeError = chromeApi.runtime?.lastError
      if (runtimeError || typeof window?.id !== "number") {
        reject(new Error(runtimeError?.message ?? "No current window"))
        return
      }
      resolve(window.id)
    })
  })
}

export async function openExtensionSidePanel(): Promise<ExtensionLaunchResult> {
  if (!supportsSidePanel()) {
    return { ok: false, code: "side_panel_unavailable" }
  }

  const sidePanelApi = globalThis.chrome?.sidePanel
  if (!sidePanelApi?.open) {
    return { ok: false, code: "side_panel_unavailable" }
  }

  try {
    const windowId = await getCurrentWindowId()
    await sidePanelApi.open({ windowId })
    return { ok: true }
  } catch {
    return { ok: false, code: "side_panel_failed" }
  }
}

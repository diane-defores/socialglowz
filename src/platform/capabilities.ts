export type PlatformCapabilities = {
  isTauri: boolean
  isExtension: boolean
  isChromeExtension: boolean
  isFirefoxExtension: boolean
  supportsSidePanel: boolean
  supportsNativeWebview: boolean
  supportsNativeSessionIsolation: boolean
  supportsHaptics: boolean
  supportsNativeBackup: boolean
}

function getUserAgent(): string {
  if (typeof navigator === "undefined" || typeof navigator.userAgent !== "string") {
    return ""
  }
  return navigator.userAgent
}

export function isTauri(): boolean {
  if (typeof window === "undefined") return false
  return "__TAURI_INTERNALS__" in window
}

export function isExtension(): boolean {
  const chromeRuntimeId = globalThis.chrome?.runtime?.id
  const browserRuntimeId = (globalThis as { browser?: { runtime?: { id?: string } } }).browser?.runtime?.id
  return typeof chromeRuntimeId === "string" || typeof browserRuntimeId === "string"
}

export function isFirefoxExtension(): boolean {
  if (!isExtension()) return false
  return /firefox/i.test(getUserAgent())
}

export function isChromeExtension(): boolean {
  if (!isExtension()) return false
  return !isFirefoxExtension()
}

export function supportsSidePanel(): boolean {
  if (!isChromeExtension()) return false
  return typeof globalThis.chrome?.sidePanel?.open === "function"
}

export function supportsNativeWebview(): boolean {
  return isTauri()
}

export function supportsNativeSessionIsolation(): boolean {
  return isTauri()
}

export function supportsHaptics(): boolean {
  return isTauri()
}

export function supportsNativeBackup(): boolean {
  return isTauri()
}

export function getPlatformCapabilities(): PlatformCapabilities {
  const tauri = isTauri()
  const extension = isExtension()
  const firefox = isFirefoxExtension()
  const chrome = extension && !firefox

  return {
    isTauri: tauri,
    isExtension: extension,
    isChromeExtension: chrome,
    isFirefoxExtension: firefox,
    supportsSidePanel: chrome && supportsSidePanel(),
    supportsNativeWebview: tauri,
    supportsNativeSessionIsolation: tauri,
    supportsHaptics: tauri,
    supportsNativeBackup: tauri,
  }
}

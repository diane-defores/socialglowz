import {
  getPlatformCapabilities,
  isChromeExtension,
  isExtension,
  isFirefoxExtension,
  isTauri,
  supportsSidePanel,
} from "@/platform/capabilities"

type GlobalSnapshot = {
  window: typeof globalThis.window | undefined
  chrome: typeof globalThis.chrome | undefined
  browser: unknown
  userAgent: string | undefined
}

function setUserAgent(userAgent: string) {
  Object.defineProperty(globalThis, "navigator", {
    configurable: true,
    value: { userAgent },
  })
}

describe("platform capabilities", () => {
  let snapshot: GlobalSnapshot

  beforeEach(() => {
    snapshot = {
      window: globalThis.window,
      chrome: globalThis.chrome,
      browser: (globalThis as { browser?: unknown }).browser,
      userAgent: globalThis.navigator?.userAgent,
    }

    delete (globalThis as { window?: unknown }).window
    delete (globalThis as { chrome?: unknown }).chrome
    delete (globalThis as { browser?: unknown }).browser
    setUserAgent("node")
  })

  afterEach(() => {
    if (snapshot.window === undefined) {
      delete (globalThis as { window?: unknown }).window
    } else {
      ;(globalThis as { window?: unknown }).window = snapshot.window
    }

    if (snapshot.chrome === undefined) {
      delete (globalThis as { chrome?: unknown }).chrome
    } else {
      ;(globalThis as { chrome?: unknown }).chrome = snapshot.chrome
    }

    if (snapshot.browser === undefined) {
      delete (globalThis as { browser?: unknown }).browser
    } else {
      ;(globalThis as { browser?: unknown }).browser = snapshot.browser
    }

    if (snapshot.userAgent) {
      setUserAgent(snapshot.userAgent)
    }
  })

  it("returns false for all extension and tauri capabilities in plain node runtime", () => {
    expect(isTauri()).toBe(false)
    expect(isExtension()).toBe(false)
    expect(isChromeExtension()).toBe(false)
    expect(isFirefoxExtension()).toBe(false)
    expect(supportsSidePanel()).toBe(false)

    expect(getPlatformCapabilities()).toMatchObject({
      isTauri: false,
      isExtension: false,
      isChromeExtension: false,
      isFirefoxExtension: false,
      supportsSidePanel: false,
      supportsNativeWebview: false,
      supportsNativeSessionIsolation: false,
      supportsHaptics: false,
      supportsNativeBackup: false,
    })
  })

  it("detects tauri when __TAURI_INTERNALS__ is present on window", () => {
    ;(globalThis as { window?: Record<string, unknown> }).window = { __TAURI_INTERNALS__: {} }
    expect(isTauri()).toBe(true)
    expect(getPlatformCapabilities().supportsNativeWebview).toBe(true)
  })

  it("detects chrome extension and side panel support", () => {
    setUserAgent("Mozilla/5.0 Chrome/123.0")
    ;(globalThis as { chrome?: unknown }).chrome = {
      runtime: { id: "abc" },
      sidePanel: { open: vi.fn() },
    }

    expect(isExtension()).toBe(true)
    expect(isFirefoxExtension()).toBe(false)
    expect(isChromeExtension()).toBe(true)
    expect(supportsSidePanel()).toBe(true)
  })

  it("detects firefox extension from user agent", () => {
    setUserAgent("Mozilla/5.0 Firefox/126.0")
    ;(globalThis as { chrome?: unknown }).chrome = {
      runtime: { id: "abc" },
    }

    expect(isExtension()).toBe(true)
    expect(isFirefoxExtension()).toBe(true)
    expect(isChromeExtension()).toBe(false)
    expect(supportsSidePanel()).toBe(false)
  })
})

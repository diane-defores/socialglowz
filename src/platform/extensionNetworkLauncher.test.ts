import {
  launchExternalUrl,
  normalizeHttpsUrl,
  openExtensionDashboard,
} from "@/platform/extensionNetworkLauncher"

type ChromeMock = {
  runtime?: {
    id?: string
    getURL?: (path: string) => string
    lastError?: { message: string }
  }
  tabs?: {
    create?: (props: { url: string }, callback?: () => void) => void
  }
}

describe("extensionNetworkLauncher", () => {
  const originalChrome = globalThis.chrome

  afterEach(() => {
    if (originalChrome === undefined) {
      delete (globalThis as { chrome?: unknown }).chrome
    } else {
      ;(globalThis as { chrome?: unknown }).chrome = originalChrome
    }
  })

  it("normalizes naked hostnames to https", () => {
    const result = normalizeHttpsUrl("example.com/path")
    expect(result).toEqual({
      ok: true,
      url: "https://example.com/path",
      host: "example.com",
    })
  })

  it("rejects forbidden and non-https schemes", () => {
    expect(normalizeHttpsUrl("javascript:alert(1)")).toEqual({
      ok: false,
      code: "forbidden_protocol",
    })
    expect(normalizeHttpsUrl("http://example.com")).toEqual({
      ok: false,
      code: "https_required",
    })
    expect(normalizeHttpsUrl("https://user:pass@example.com")).toEqual({
      ok: false,
      code: "credentials_not_allowed",
    })
  })

  it("returns tabs_api_unavailable when no extension tabs API exists", async () => {
    delete (globalThis as { chrome?: unknown }).chrome
    await expect(launchExternalUrl("https://example.com")).resolves.toEqual({
      ok: false,
      code: "tabs_api_unavailable",
    })
  })

  it("opens tabs via chrome.tabs.create when URL passes validation", async () => {
    const tabsCreate = vi.fn((_: { url: string }, callback?: () => void) => callback?.())
    const chromeMock: ChromeMock = {
      runtime: { id: "abc" },
      tabs: { create: tabsCreate },
    }
    ;(globalThis as { chrome?: unknown }).chrome = chromeMock as unknown as typeof globalThis.chrome

    await expect(launchExternalUrl("https://x.com")).resolves.toEqual({ ok: true })
    expect(tabsCreate).toHaveBeenCalledWith({ url: "https://x.com/" }, expect.any(Function))
  })

  it("opens extension dashboard route through runtime.getURL", async () => {
    const tabsCreate = vi.fn((_: { url: string }, callback?: () => void) => callback?.())
    const chromeMock: ChromeMock = {
      runtime: {
        id: "abc",
        getURL: (path: string) => `chrome-extension://abc/${path}`,
      },
      tabs: { create: tabsCreate },
    }
    ;(globalThis as { chrome?: unknown }).chrome = chromeMock as unknown as typeof globalThis.chrome

    await expect(openExtensionDashboard("/setup/update")).resolves.toEqual({ ok: true })
    expect(tabsCreate).toHaveBeenCalledWith(
      { url: "chrome-extension://abc/src/ui/setup/index.html#/setup/update" },
      expect.any(Function),
    )
  })
})

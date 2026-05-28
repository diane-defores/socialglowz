import manifest from "../../manifest.config"

describe("extension manifest baseline", () => {
  it("does not ship global content script or devtools page by default", () => {
    expect(manifest.content_scripts).toBeUndefined()
    expect(manifest.devtools_page).toBeUndefined()
  })

  it("uses minimum baseline permissions", () => {
    expect(manifest.permissions).toEqual(expect.arrayContaining(["storage", "tabs"]))
    expect(manifest.permissions).not.toEqual(expect.arrayContaining(["background", "sidePanel"]))
  })
})

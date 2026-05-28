import { defineManifest } from "@crxjs/vite-plugin"
import ManifestConfig from "./manifest.config"

// @ts-expect-error ManifestConfig provides all required fields
export default defineManifest((env) => ({
  ...ManifestConfig,
  browser_specific_settings: {
    gecko: {
      id: env["FIREFOX_ADDON_ID"] || "socialglowz@local",
      data_collection_permissions: {
        required: ["none"],
      },
    },
  },
  icons: {
    800: "src/assets/logo.png",
  },
  background: {
    scripts: ["src/background/index.ts"],
    type: "module",
    persistent: false,
  },
  permissions: [...ManifestConfig.permissions],
}))

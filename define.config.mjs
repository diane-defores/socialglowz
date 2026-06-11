import fs from "node:fs"
import { spawnSync } from "node:child_process"
import packageJson from "./package.json" with { type: "json" }

// Read CHANGELOG.md file into a string.
const changelog = fs.readFileSync("./CHANGELOG.md", "utf-8")

// Get the current git commit hash.
const gitCommit = spawnSync("git", ["rev-parse", "--short", "HEAD"])
  .stdout.toString()
  .trim()
const buildAtUtc = process.env.BUILD_AT_UTC || new Date().toISOString().replace(/\.\d{3}Z$/, "Z")
const buildAtParis = process.env.BUILD_AT_PARIS || new Intl.DateTimeFormat("sv-SE", {
  timeZone: "Europe/Paris",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
}).format(new Date()).replace("T", " ")
const buildId = process.env.BUILD_ID || process.env.GITHUB_RUN_ID || process.env.VERCEL_GIT_COMMIT_SHA || gitCommit || "unknown"

const jsn = (value) => JSON.stringify(value)

// Don't forget to add your added variables to vite-env.d.ts also!

// These variables are available in your Vue components and will be replaced by their values at build time.
// These will be compiled into your app. Don't store secrets here!

export const defineViteConfig = {
  __VERSION__: jsn(packageJson.version),
  __NAME__: jsn(packageJson.name),
  __DISPLAY_NAME__: jsn(packageJson.displayName),
  __CHANGELOG__: jsn(changelog),
  __GIT_COMMIT__: jsn(gitCommit),
  __BUILD_ID__: jsn(buildId),
  __BUILD_AT_PARIS__: jsn(buildAtParis),
  __BUILD_AT_UTC__: jsn(buildAtUtc),
  __GITHUB_URL__: jsn(packageJson.repository.url),
  // Set the HTML title for all pages from package.json so you can use %HTML_TITLE% in your HTML files.
  HTML_TITLE: jsn(packageJson.displayName),
}

# Tauri 2 Mobile on a Server

This project already uses Tauri 2:

- JS CLI/API: `@tauri-apps/cli` 2.x, `@tauri-apps/api` 2.x
- Rust: `tauri` 2.x

## Why mobile prerequisites are heavy

Tauri mobile wraps native toolchains:

- Android needs Java + Android SDK/NDK
- iOS needs Xcode, which only runs on macOS

So a Linux server cannot build iOS locally.

## Practical setup when you dev on a server

1. Keep coding on your server (frontend + Rust shared logic).
2. Build desktop Linux on server/CI (already configured in `.github/workflows/build.yml`).
3. Build Android in CI on Blacksmith Ubuntu runners through GitHub Actions.
4. Build iOS in CI on macOS runners.

## Hetzner + Termux workflow (recommended)

If your code runs on a remote Linux server and your phone is not directly connected to that server:

1. Push your branch to GitHub.
2. Run the workflow `Dev Builds (Android + Windows)` (manual or any `git push`) on GitHub Actions / Blacksmith.
3. Download the artifact `socialglowz-android-debug`.
4. Install the APK on your Android phone and test.

This avoids local Android Studio/SDK setup on your server machine.

## Android WebView session QA

For Android session-isolation checks, use the APK built by GitHub Actions on Blacksmith:

1. Push your branch to GitHub.
2. Run the workflow `Dev Builds (Android + Windows)` if it did not start from the push.
3. Download the artifact `socialglowz-android-debug`.
4. Install the APK on the Android phone.
5. Validate that embedded networks keep cookies and localStorage snapshots separated by `${profileId}-${networkId}`.

CinderReels is the reference manual test because its authentication uses localStorage. A valid test switches Profile A -> Profile B -> Profile A and confirms each profile returns to its own CinderReels account without showing the other profile's state.

Known limits for this Android WebView isolation: IndexedDB, CacheStorage, service workers, global WebView HTTP cache and system credential stores are not covered.

## Windows desktop test workflow

To test on Windows without building locally:

1. Push your branch to GitHub.
2. Run `Dev Builds (Android + Windows)` workflow.
3. Download artifact `socialglowz-windows-test`.
4. Install `.msi` or run `.exe` on your Windows machine.

## NPM scripts (already added)

- `pnpm tauri:info`
- `pnpm tauri:android:init`
- `pnpm tauri:android:dev`
- `pnpm tauri:android:build`
- `pnpm tauri:ios:init`
- `pnpm tauri:ios:dev`
- `pnpm tauri:ios:build`

## Notes

- `ios` commands require a macOS machine/runner with Xcode.
- If you cannot install Android SDK locally, run Android build in CI only. For this project, Android manual QA is considered authoritative only after installing the APK artifact produced by the GitHub Actions / Blacksmith dev build.
- For signing/publishing, add keystore/certificate secrets in CI.
- `tauri android dev` with live reload needs a machine that has:
  - Java + Android SDK/NDK
  - `adb` access to a real Android device (or emulator)

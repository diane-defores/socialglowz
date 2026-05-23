# ShipFlow

## ShipFlow Development Mode

- development_mode: hybrid
- validation_surface: mixed
- ship_before_preview_test: conditional
- post_ship_verification: GitHub Actions artifact install
- deployment_provider: GitHub Actions on Blacksmith runners
- preview_source: `Dev Builds (Android + Windows)` workflow artifact `socialglowz-android-debug`
- production_url: unknown
- notes: Android behavior is validated only from APKs built by GitHub Actions on Blacksmith (`.github/workflows/dev-builds.yml`), then installed on a real Android device. Local `pnpm tauri:android:build` is not authoritative in this repo because the generated Android project is created in CI.
- last_reviewed: 2026-05-23


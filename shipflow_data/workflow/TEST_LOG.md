# Test Log

## 2026-05-24 13:39 UTC - Android WebView pooling manual QA

- Skill: sf-test
- Environment: Android APK installed on real phone from GitHub Actions / Blacksmith workflow artifact, per project development mode.
- Scope: `shipflow_data/workflow/specs/android-webview-pooling-fast-switching.md`
- Tester: Diane
- Scenario: Test 1, same-profile fast switch.
- Steps reported:
  1. Open Profile A network A.
  2. Switch to another network.
  3. Return to the first network.
- Expected: return to the warm WebView host with no visible full reload and near-instant display.
- Observed: return takes about 4 seconds and visibly reloads every time.
- Result: FAIL_LOADING / FAIL_PERFORMANCE
- Evidence supplied: user report in chat; no copied app logs, logcat, Sentry event, device model, or artifact commit yet.
- Linked bug: `shipflow_data/workflow/bugs/BUG-2026-05-24-001.md`
- Next step: collect Android SFZ logs around `open_webview` / `show_webview` / `hide_webview`, then route to sf-fix.

### Follow-up results

- 2026-05-24 13:42 UTC - Test 2, profile isolation between Profile A and Profile B: PASS.
- 2026-05-24 13:42 UTC - Test 3, returning to Profile A after Profile B: PASS.
- Interpretation: session isolation appears OK from manual observation; the current blocker remains fast-switch pooling/reload performance from Test 1.

### 2026-05-24 13:53 UTC - Copied SFZ log analysis

- Evidence source: user copied Android in-app `SFZ` debug logs.
- Relevant log lines:
  - `⇄ SWITCH facebook → instagram`
  - `cookies saved for session (...)`
  - `cookies restored for session (...)`
  - `loadUrl: https://instagram.com`
  - `⇄ SWITCH instagram → facebook`
  - `loadUrl: https://facebook.com`
- Interpretation: the installed APK is using the old/native switch path that reloads by design. The copied logs also include `reuse existing webview (switch)`, a debug line absent from the current source tree, which indicates the installed APK is not the current pooling implementation or was built from an older revision.
- Result: BLOCKED_STALE_APK for validating the new pooling code; the observed reload remains valid for the installed APK.

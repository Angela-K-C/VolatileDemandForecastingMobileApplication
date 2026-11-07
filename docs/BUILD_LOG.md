# Build log

All work happened on 2026-11-07 on `main`, one commit per step. The brief: read `SemProject_MLEngine/MOBILE_APP_HANDOFF.md`, the design spec it links to and `SemProject_Backend/docs/api.md`, then build the app in the handoff's order and stop after each step for a manual check. Steps 1–3 were checked by hand on a physical phone (Infinix X669, Android 12) before the next began. Steps 4–8 were built and verified by automated checks; their signed-in behaviour is still to be checked by hand.

## Commits

| Commit | Step | Summary |
|--------|------|---------|
| `37abc76` | 1 Foundation | Theme, shell, navigation, the three menus, state components |
| `9c924ce` | 1 fix | Fixed infinite recursion on launch in `ComposeFragment` |
| `da92bd7` | 2 Auth | Sign in, register, refresh, sign out, password reset |
| `a65836b` | 3 Demo journey | Workspace picker, dashboard, products, forecast flow, history, model info |
| `9030ddb` | 4 Business journey | Create workspace, import wizard, readiness, eligibility, training |
| `db01474` | 5 Team and settings | Members, invitations, settings, change password, help |
| `d5bafb7` | 6 Research | Research summary and CSV export |
| `4724160` | 7 Offline | Room response cache |
| `d2c2ee7` | 8 Acceptance | Lint fixes, guard tests, `docs/ACCEPTANCE.md` |

Earlier history (`385522b`, `b938ba4`, `3527e64`, `bfc666f`) is the initial project, README and `CLAUDE.md`. Everything is pushed to `origin/main`.

## Standing rules (from the brief and `CLAUDE.md`)

- Never add `Co-Authored-By` or "Generated with…" lines to commits or pull requests.
- Do not modify `../SemProject_Backend` or `../SemProject_MLEngine`; raise API contract problems with the project owner.
- Never use the user's real credentials or create accounts. Authenticated checks need the user to sign in on the phone.
- Commit locally as work goes; push only when asked (pushed once, after step 8).
- Spec rules: no XML screen layouts; no forecasting maths in the app; build from `capabilities`; never fabricate data; be honest about results; `null` units are gaps; paired metrics; the "lower error" caveat; the ~4% high 7-day note; demo is read-only.

## Step 1: Foundation

Built: project setup with the version catalog; theme and design tokens (light/dark); `MainActivity` with a Fragment host; the `ComposeFragment` base; the shell (top bar, workspace chip, bottom bar); the Options, context and popup menus; reusable components (buttons, fields, chips, inline messages, scaffold); the state set Loading/Empty/Error/Offline; and a debug-only component gallery.

Problem: the app recursed forever on launch because the base class's `Content()` called itself instead of the Fragment's. Fixed by naming the Fragment explicitly (`this@ComposeFragment.Content()`), commit `9c924ce`.

Pin: kotlinx-serialization is held at 1.10.0 because newer versions need Kotlin 2.4. An unclosed comment and a 422 email-handling bug were also fixed during this step.

## Step 2: Authentication

Built: sign in, register, forgot-password (request code, then confirm) and sign out; an OkHttp `Authenticator` that refreshes the access token once for concurrent callers; a Keystore-backed (AES-GCM) token store; a `SessionManager` that drives routing; friendly error mapping (`ErrorMessages`) including rate-limit waits and unknown-code fallback to the API message; inline form validation using the backend's limits.

Decisions: logout is local-first (the user is signed out immediately, the server call runs on an application scope and may fail silently); the public client carries auth-free endpoints so a stale token can never trigger a refresh during logout; password-reset requests always look successful because the backend does not reveal whether an account exists.

## Step 3: Demo journey

Built: workspace picker with the Reference workspace marked as demo; dashboard; paged product list with search and demand-class filter; product detail with a history chart; forecast configuration driven by `capabilities`; job creation and polling; the result screen (chart, table, totals, verdict, three kinds of evidence); forecast history with filters, retry, delete and CSV export; model info (the model card). Added DataStore for the remembered store selection and the number-display preference, and a custom Canvas `LineChart`.

Decisions:
- Polling: every 1 s for 10 s, then every 3 s, up to 2 minutes.
- Totals are summed from raw API values; rounding is display-only.
- A verdict is stated plainly even when the baseline wins; the metrics block always puts "Lower error means it did better on that sample, not that it will do better tomorrow." beneath it.
- "Error on these days" appears for replays only, and each block keeps its AI and baseline columns from one API object.

Problems fixed: a missing `dp` import in the picker, and the deprecated `Icons.Outlined.OpenInNew` (replaced by the auto-mirrored version). 35 tests passed at the end of this step.

## Step 4: Business journey

Built:
- **Create workspace and store** in one form, with local checks for country (2 letters), currency (3 letters) and time zone (must be a real zone ID). The workspace id is kept after a store failure so retrying does not create a second workspace.
- **Data tab** with history summary, status-filtered import list, paging, delete via long-press, and a silent refresh when returning to the screen.
- **Import wizard** (7 steps) with SAF file picking, mapping pre-fill and "Please check" flags, date-format choice only when ambiguous, a zero-sales question with no default, validation report with line numbers, overlap step skipped when nothing overlaps, confirm, and a result screen that fetches readiness.
- **Readiness** screen with the API's thresholds and per-product reasons.
- **Training** section in Model info: eligibility checks, Train, polling every 2.5 s up to 10 minutes, and the four outcomes including a plain `GATE_FAILED` message.
- **Dashboard** eligibility progress line, Upload action, onboarding text.
- A **data revision** signal in `ActiveStore` so screens reload after a change.

Decisions: the "21 days" wording was removed from an error message because thresholds must come from the API; going Back from the mapping step leaves the wizard because the file is already uploaded (the server discards an unconfirmed import); the stale-history notice is hidden for the demo store.

Problem: scripted edits with quote-heavy strings failed in shell heredocs; the workaround was writing fragments with the file tool and merging with a short isolated Python script. 46 tests at the end of this step.

Sample-file expectations are in the README.

## Step 5: Team and settings

Built: Team screen (invite, change role, remove, revoke, leave), My invitations (accept/decline) with a Settings badge, Settings (profile, workspace and store details, theme, number display, links), Change password, Help. Theme is now a stored preference applied by `ComposeFragment`. The Options menu opens these screens instead of the earlier placeholders.

Decisions: the demo workspace has no team screen content; the Reference role is never assignable; leaving a workspace clears the active selection and returns to the picker; a wrong current password appears on that field; API codes `LAST_OWNER`, `ALREADY_MEMBER` and `INVITATION_NOT_PENDING` have friendly messages.

Not done: no second account was created, so the invitation flow and the Viewer permissions (demo script 6) have not been run end to end. 49 tests.

## Step 6: Research

Built: Research summary (overall, by demand type, by day ahead; three methods per block; the API's note) and CSV export through the file dialog, with an optional as-of date range validated before sending. The menu entry shows only when `isResearch` is true.

Decision: the verdict sentence under each block compares AI with "same as last day" only; the seasonal column is shown beside it but is not part of the sentence. 53 tests.

## Step 7: Offline and cache

Built: Room with KSP (2.8.4 / 2.3.6, which resolved fine with AGP 9.3.3 and Kotlin 2.2.10), a read-through `ResponseCache`, caching in the product, import and forecast repositories, an offline notice with the saved time, pruning after 30 days, and clearing on sign-out. Details in `ARCHITECTURE.md`.

Decisions: a live answer is always preferred and a real server error is never replaced by an old copy; only completed forecasts are cached (immutable); screens opt in to the notice. 59 tests.

Problem: the first launch check used the monkey launcher, which did not open the app; starting `MainActivity` directly showed it running with no crash.

## Step 8: Tests and acceptance

Built: lint run and fixes (two real errors: a resource lookup through `LocalContext`, and a `%` in the 4% limitation text read as a format string; plus locale-aware date formatters and five unused strings removed); `AcceptanceGuardTest` (no XML layouts, valid nav graph, manifest rules, no URLs in source); `docs/ACCEPTANCE.md`. 64 tests; lint 0 errors, 42 warnings (mostly plural candidates and newer-library notices).

## Environment notes

- The phone is connected by USB. After every re-plug run `adb reverse tcp:8000 tcp:8000`. `local.properties` has `api.baseUrl=http://localhost:8000`.
- adb is at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. In Git Bash set `MSYS_NO_PATHCONV=1` so `/sdcard` paths are not rewritten.
- Screenshots: `adb shell screencap -p /sdcard/s.png`, then `adb pull`.
- Long-running commands should use a timeout; a launcher command once ran past the tool's limit and had to be finished in the background.

## Open items

1. Manual pass of steps 4–8 signed in (see `docs/ACCEPTANCE.md` and the README sample-file table). Demo scripts 6 (team) and 7 (password reset) need a second account and backend logs.
2. Convert the 28 `%d`-plus-unit strings to plurals.
3. Add a seam for `ActiveStore` (an interface or a fake `Context`) so ViewModels can be unit tested; consider instrumented UI tests for sign-in and navigation.
4. Extend the offline cache to the dashboard, product history and model screens if wanted; consider automatic upload retry.
5. Upgrade Kotlin, Compose BOM and kotlinx libraries together (lint lists newer versions).
6. Accessibility pass with TalkBack and large fonts (48 dp targets and chart text summaries are not machine-checked).

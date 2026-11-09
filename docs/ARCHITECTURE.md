# Architecture

## Stack

| Concern | Choice |
|---------|--------|
| Language / UI | Kotlin 2.2.10, Jetpack Compose with Material 3 |
| Build | AGP 9.3.3, Gradle version catalog (`gradle/libs.versions.toml`) |
| Screens | One Fragment per destination, each hosting a `ComposeView` (no XML layouts) |
| Navigation | Navigation Component with an XML nav graph (`res/navigation/main_graph.xml`) |
| Pattern | MVVM; `StateFlow` in ViewModels, `collectAsStateWithLifecycle` in Compose |
| Networking | Retrofit 3.0.0, OkHttp 5.5.1, kotlinx-serialization 1.10.0, coroutines 1.10.2 |
| Preferences | DataStore 1.3.0 (workspace/store selection, theme, number display) |
| Offline cache | Room 2.8.4 with KSP 2.3.6 |
| Secrets | Android Keystore-backed token store |
| DI | Hand-wired `AppContainer` (no framework) |

Version notes: kotlinx-serialization is pinned at 1.10.0 because newer releases are built with Kotlin 2.4 and cannot be read by Kotlin 2.2. Lint reports newer versions of several libraries; upgrading them together with Kotlin is a separate task.

## Package map (`com.example.volatiledemandforecasting`)

```
app/            AppContainer (DI), MainActivity host pieces, ComposeFragment (base),
                ShellFragment + ShellViewModel (top bar, menus, bottom bar), OfflineCopyNotice,
                navigation/ (MainTab, tab switching)
core/
  network/      Network (Retrofit/OkHttp/Json), ApiError + ApiErrorCodes, ErrorMessages
                (error code → string), TokenAuthenticator, Interceptors
  security/     SessionManager, TokenStore, KeystoreTokenStore
  datastore/    AppPreferences (StoreSelection, ThemeMode, TableDisplay, tourSeen)
  ui/           Formatting, UiText, theme/, components/ (scaffold, buttons, fields, chips,
                menus, state views, InfoHint "?" explanations), charts/ (custom Canvas LineChart)
data/           One package per backend area, each with DTOs + Retrofit API + repository:
                auth, user, workspace (+ ActiveStore), store (+ ModelCard), catalog (products),
                forecast (incl. batches), imports, training, team, cache (Room),
                sample (SampleSalesFile: the built-in demo CSV, generated on the phone)
feature/        One package per screen group: onboarding (intro tour), auth, workspace (+ create,
                welcome), dashboard (+ GettingStarted checklist), products (+ detail), forecasts
                (store-wide, config, progress, result, history), model, data (+ wizard, readiness),
                team, settings (+ help), devgallery (debug only)
```

## Layering

`Fragment (Compose)` → `ViewModel` → `Repository` → `Retrofit API`. Repositories return DTOs and throw; ViewModels turn results into UI state. Hand-wiring happens once in `AppContainer`; ViewModels are created through `appViewModels { }` / `appActivityViewModels { }` helpers that pass the container to a factory lambda.

## Application shell

`ComposeFragment` is the base class for every destination: it creates the `ComposeView`, applies the theme (light/dark/system from preferences) and calls the subclass's `Content()`.

`MainActivity.route` decides the start screen from the session and `AppPreferences.tourSeen`: the intro tour on first launch (signed in or not), then Sign in or the Dashboard. The tour sets `tourSeen` when it ends; Help can open it again as a replay, which simply pops back.

`ShellFragment` extends it for screens inside the signed-in app. It provides:
- a top bar with title, back arrow (supporting screens), the **workspace chip**, an optional contextual action, and the **Options menu**;
- a bottom navigation bar when `tab` is set (Dashboard, Products, Data, Forecasts);
- the sign-out confirmation and About dialog;
- `collectSelection()` for the active store, and the shared `ShellViewModel` (profile, role, pending-invitation badge);
- the offline notice, shown above screens that declare `showsCachedData = true`.

Three menu types (spec §6): **options** (top-bar overflow), **context** (long-press on list rows), **popup** (filter and sort choices).

## State handling

- `UiState<T>`: `Loading`, `Content`, `Empty`, `Error(UiText)`, `Offline`. `UiStateHost` renders each with a retry action. `Throwable.toUiState()` maps `IOException` to `Offline`.
- `attempt { }` wraps a call in a `Result` (cancellation is re-thrown). `UiText` carries either a string resource (with args) or literal text, so ViewModels stay free of `Context`.
- `StoreGate` handles "loading selection / no store chosen / store selected" for screens that need a store.

## Active workspace and store

`ActiveStore` (DataStore-backed) exposes the current `StoreSelection` (workspace, role, store, `isReference`). It also has a **data revision** counter: after an import, deleted import or finished training run, `dataChanged()` bumps it and the Dashboard, Products, Readiness, Training and Model screens reload instead of showing stale numbers.

Permissions derive from the selection: `canGenerateForecasts` (reference store, or any role except Viewer) and `canManageData` (own workspace and Owner/Manager). The Reference (demo) workspace is read-only for data.

## Networking and authentication

- `Network.json` ignores unknown keys and treats nulls as absent. Read and write timeouts are 90 s (forecast and training requests can be slow).
- A `RequestIdInterceptor` adds a request id to every call, so a failure can be traced from the app to the backend. Nothing logs bodies, tokens or sales data (there is no HTTP logging).
- Two clients: a **public** client for sign-in, register, refresh, logout and password reset; an **authed** client that adds the bearer token.
- `TokenAuthenticator` refreshes on a 401 using the refresh token, serialised so concurrent requests share one refresh. A failed refresh signs the user out with a reason shown on the sign-in screen.
- Logout clears the session locally first, then tells the server on an app-lifetime scope; if offline, the refresh token simply expires.
- `apiCall { }` converts `HttpException` into `ApiException(httpStatus, code, message, details, requestId, retryAfterSeconds)`. Transport failures remain plain `IOException`.
- `ErrorMessages` maps API error codes to friendly string resources and falls back to the API's own `message` for unknown codes. Server field errors on `422 INVALID_REQUEST` are mapped to the named form fields.
- Tokens are stored encrypted with a Keystore key. Sign-out clears tokens, the workspace selection and the Room cache.

## Forecast flow

1. Configuration is built from `GET …/models/capabilities` (methods, horizons, as-of rules, baseline labels). Nothing is hardcoded.
2. `POST` creates a job; the progress screen polls it (`PollingSchedule`: every 1 s for the first 10 s, then every 3 s, up to 2 minutes). States are honest: queued, running, completed, failed (with retry only when allowed).
3. The result screen builds the chart, table, totals and verdicts from the API response (`buildChartModel`, `totalsOf`, `verdictOf`). Totals are summed from raw values; display rounding is separate (one decimal by default, whole units optional).
4. Evidence is shown in three kinds and never mixed: the product's backtest, "error on these days" (replays only), and per-day forecast vs. actual. `MetricsBlock` always takes its AI and baseline columns from the same API object.
5. Which number a shop owner sees is decided by `shownMethodOf` (`data/forecast/ShownForecast.kt`): AI only when the store's model card backtest shows the AI with a lower RMSE (or MAE) than "same as last day"; otherwise the simple forecast. `usedFor` falls back to the simple forecast for a product without AI values. Totals never treat a missing day as zero.
6. The Forecasts tab uses store-wide batches (`BatchRepository`): one request queues a forecast per product; the screen polls the batch every 2.5 s, then pages through its items. Each item is an ordinary forecast and opens on the result screen.
7. Completed forecasts export to CSV via the Storage Access Framework (`CreateDocument`), streamed straight to the chosen file.

## Import and training flow (business journey)

Seven wizard steps: choose file → check file → map columns → validation → overlap policy (skipped if no overlap) → confirm → result.
- The file is read through the Storage Access Framework (no storage permission) and uploaded as multipart with an `Idempotency-Key` that is new per picked file, so retries do not create duplicate imports.
- Mapping is pre-filled from the server's suggestions; low-confidence ones are flagged "Please check". The date-format choice appears only when the file's dates are ambiguous. "Does a missing row mean zero sales?" has **no default** and must be answered.
- Validation, readiness thresholds and training eligibility all use numbers the API returns (`required`, `minHistoryDays`, etc.).
- Training polls every 2.5 s for up to 10 minutes. Outcomes: `TRAINED`, `GATE_FAILED` (stated plainly with the numbers and the baseline staying in use), `NOT_ELIGIBLE`; retry only when the run says `retryable`.

## Offline cache

`ResponseCache` (Room, one table of JSON payloads keyed by request) is a **read-through** cache: the live answer is always preferred and saved; only an `IOException` falls back to the saved copy. Real server errors are never replaced by an old copy.
- Cached: product pages and single products, import lists, forecast history pages, **completed** forecasts only (they never change).
- Not cached: dashboard, product history charts, readiness, model screens.
- A saved copy raises a notice with its fetch time ("Last synced") on screens that opt in; any live success clears it.
- Entries older than 30 days are pruned at app start. Sign-out clears everything. The schema may be destroyed and rebuilt on upgrade because it only holds copies of server data.

## UI conventions

- Theme tokens (colors, spacing, shapes, typography) live in `core/ui/theme`; light and dark schemes; user-selectable in Settings.
- Status is never colour-only: chips and charts carry text, markers and line styles.
- The chart is a custom Canvas `LineChart` (solid, dashed, dotted and dot styles). `units: null` leaves a gap.
- All user-visible text is in `res/values/strings.xml`. Dates and numbers are formatted per locale on each call.
- The debug-only `devgallery` screen shows the shared components.

## Security and privacy

- No secrets or URLs in source (enforced by `AcceptanceGuardTest`); the API base URL comes from the git-ignored `local.properties`.
- Release manifest: only the INTERNET permission, no cleartext traffic.
- Logs carry no tokens, passwords or sales data.
- Sign-out clears user-scoped local data.

## Tests

79 JVM unit tests under `app/src/test`:

| Area | Tests |
|------|-------|
| Error mapping | `ErrorMessagesTest` |
| Token refresh, concurrency, sign-out | `TokenAuthenticatorTest` (MockWebServer) |
| Auth form rules | `AuthValidationTest` |
| Forecast API contract and polling | `ForecastContractTest` |
| Chart/totals/verdict/formatting | `ForecastPresentationTest` |
| Import wizard rules, upload request shape, DTO decoding, training/eligibility, store form checks | `ImportWizardTest` |
| Team DTOs, assignable roles, password confirm | `TeamSettingsTest` |
| Shown method (AI vs simple), totals, batch DTOs, sample-shop flag | `StoreForecastTest` |
| Getting started checklist: steps, next step, AI stages, when it shows | `GettingStartedTest` |
| Built-in sample sales file: header, products, days, determinism (writes a copy when `SAMPLE_SALES_OUT` is set; run with `--rerun`) | `SampleSalesFileTest` |
| Offline cache rules (fake DAO) | `ResponseCacheTest` |
| Source-tree guarantees (no XML layouts, valid nav graph, manifest, no URLs) | `AcceptanceGuardTest` |

Not covered: instrumented UI tests, and most ViewModels (several depend on `ActiveStore`, which needs an Android `Context`). See `docs/ACCEPTANCE.md`.

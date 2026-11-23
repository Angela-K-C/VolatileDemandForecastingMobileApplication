# Acceptance checklist (spec §19)

Status key: **Auto** = proved by a unit test or lint in this repo. **Phone** = needs a signed-in person using the app against the local backend. Nothing here has been run signed in by the developer tooling; the Phone items are open until someone ticks them.

Run the automatic checks: `./gradlew lintDebug testDebugUnitTest` (79 tests, lint 0 errors).

The spec's checklist was written for the research version. On `main` (the business version) the demo store is the **sample shop**, results lead with one number, and replays, model-version choice and the research summary are gone; rows 5 and 6 are checked on the `research` branch. The business version's own checks follow the main table.

| # | Criterion | Status | Evidence / how to check |
|---|-----------|--------|-------------------------|
| 1 | Launches and routes after splash | Phone | Cold start signed out → Sign in; signed in → Dashboard. |
| 2 | Only permitted workspaces; demo always available and marked | Phone | Your shops lists only your shops, with **Open the sample shop**; with no shop, the welcome screen offers **Try a sample shop**. The sample shop carries the "Sample shop: example data" banner. A second user does not see your shop. |
| 3 | Compose only, no XML screen layouts, Fragments host `ComposeView` | Auto | `AcceptanceGuardTest`: no `res/layout`; every nav destination is a Fragment; no dangling destination ids. |
| 4 | Options, context and popup menus with correct triggers | Phone | Options: top-bar overflow. Context: long-press a product, import or readiness row. Popup: filter and sort chips. |
| 5 | Next-day forecast for any of the 1,437 demo products, AI beside baseline | Phone | `main`: sample shop → any product → forecast; the result shows one number and **How accurate is this?** shows AI beside both baselines. `research`: handoff §8 script 2. |
| 6 | Replay 2016-04-24..2016-05-21 shows forecast vs actual and model `1.1.0-bt` | Phone (`research` branch) | Handoff §8 script 1: `FOODS_3_090`, as-of 2016-05-08 → AI ≈ 57.1, last-day 77, actual 72. Run details show the model version. Leakage refusal (`NO_MODEL_FOR_AS_OF`) is research-only. `main` has no replays. |
| 7 | ML parity tests | n/a | Lives in `SemProject_MLEngine` (already done). |
| 8 | Upload CSV, map (incl. zero-vs-missing), validate, confirm, readiness updates | Phone | Handoff §8 scripts 3–5. Request shapes (multipart `file`, `Idempotency-Key`, mapping body) are covered by `ImportWizardTest`. |
| 9 | Own data gets baselines; AI shown as not yet available with the reason | Phone | Script 4: "60 of 120 days", Train disabled, baseline forecast works. |
| 10 | Honest async job states; completed results reopen from history, incl. offline | Phone + Auto | Polling schedule and job contract: `ForecastContractTest`. Cache fallback rules: `ResponseCacheTest`. Offline: open items online, switch to airplane mode, reopen from History. |
| 11 | Every number comes from the backend; metrics state window, series, model version; only where actuals exist | Phone + Auto | `ForecastPresentationTest` (pairing, verdicts, totals from raw values). Check result screens show window and model version. |
| 12 | Model info shows card: training data, horizon, backtest, limitations | Phone | Options → AI model on the sample shop (`main` shows the verdict, training period and past accuracy; the full card is on `research`). |
| 13 | No secrets in app or repo; ML service not public | Auto (app side) | `AcceptanceGuardTest`: no URLs in source, only INTERNET permission, cleartext only in the debug manifest. `local.properties` is git-ignored. Tokens use Keystore-backed storage. ML service exposure is a backend/deployment matter. |

## Business version checks (`main`)

| Area | Status | Evidence / how to check |
|------|--------|-------------------------|
| Intro tour on first launch; Skip; ends in Create an account or Sign in; replay from Help | Phone + Auto | Fresh install. Routing: `MainActivity.route`; the destination is a Fragment (`AcceptanceGuardTest`). |
| Getting started checklist follows the shop's real state, including "train it yourself" (the first model is never trained automatically) | Phone + Auto | `GettingStartedTest` (steps, next step, AI stages, when the checklist shows). By hand: new shop → upload → forecast → train. |
| Built-in sample sales file: valid, deterministic, ends yesterday, trains and passes the gate | Auto + Phone | `SampleSalesFileTest` (header, 26 products, 300 days, same output for the same end date). Trained through the ML engine's own-data path for three end dates: gate passed, horizons 1 and 7 (`docs/demo/DEMO_GUIDE.md`). By hand: Data → Upload sales data → Use the sample sales file. |
| Store-wide forecast: one request, progress, every product's number, says AI or simple forecast | Phone + Auto | `StoreForecastTest` (which method is shown, totals, batch DTOs). By hand: Forecasts → Forecast all products. |
| "?" hints and empty states | Phone | Dashboard AI card, Forecasts, accuracy tables, Readiness, AI model; Products and Forecasts before any sales. |

## Other spec requirements checked

| Area | Status | Notes |
|------|--------|-------|
| §13.3 Offline: saved copy with "last synced", never shown as new | Phone + Auto | Notice shows the fetch time; cleared on live data. |
| §14 Sign-out clears local cache | Auto + Phone | `ResponseCacheTest` (clear); sign out then go offline as another user. |
| §16 Locale-aware dates/numbers | Auto | Lint `ConstantLocale` fixed; formatters read the locale per call. |
| §16 Strings in resources | Auto | Lint clean of hardcoded text; one bullet glyph in model info. |
| §16 48 dp touch targets, chart text summaries | Phone | Not machine-checked. Use TalkBack on a forecast result and the product history chart. |
| §16 Font scaling | Phone | Set system font to Largest and open Dashboard, wizard mapping, result screen. |

## Known gaps (not acceptance blockers, but open)

- 28 strings use `%d` followed by a unit word and should become plurals ("1 days" reads wrong). Lint: `PluralsCandidate`.
- No automated UI (instrumented) tests. Signed-in flows cannot be scripted without credentials.
- The ViewModel layer has little direct test coverage: `ActiveStore` needs an Android `Context`, so ViewModels that use it are not unit-testable without a fake or Robolectric.
- Dashboard, product history charts, readiness and model screens are not cached for offline.
- No automatic upload retry when the connection returns.

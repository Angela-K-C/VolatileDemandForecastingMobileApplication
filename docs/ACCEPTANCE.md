# Acceptance checklist (spec §19)

Status key: **Auto** = proved by a unit test or lint in this repo. **Phone** = needs a signed-in person using the app against the local backend. Nothing here has been run signed in by the developer tooling; the Phone items are open until someone ticks them.

Run the automatic checks: `./gradlew lintDebug testDebugUnitTest` (64 tests, lint 0 errors).

| # | Criterion | Status | Evidence / how to check |
|---|-----------|--------|-------------------------|
| 1 | Launches and routes after splash | Phone | Cold start signed out → Sign in; signed in → Dashboard. |
| 2 | Only permitted workspaces; demo always available and marked | Phone | Workspace picker lists the demo with a "demo" mark; a second user does not see your workspace. |
| 3 | Compose only, no XML screen layouts, Fragments host `ComposeView` | Auto | `AcceptanceGuardTest`: no `res/layout`; every nav destination is a Fragment; no dangling destination ids. |
| 4 | Options, context and popup menus with correct triggers | Phone | Options: top-bar overflow. Context: long-press a product, import, member or forecast row. Popup: filter and sort chips. |
| 5 | Next-day forecast for any of the 1,437 demo products, AI beside baseline | Phone | Handoff §8 script 2 (and any other product). |
| 6 | Replay 2016-04-24..2016-05-21 shows forecast vs actual and model `1.1.0-bt` | Phone | Handoff §8 script 1: `FOODS_3_090`, as-of 2016-05-08 → AI ≈ 57.1, last-day 77, actual 72. Run details show the model version. Leakage refusal (`NO_MODEL_FOR_AS_OF`) is research-only. |
| 7 | ML parity tests | n/a | Lives in `SemProject_MLEngine` (already done). |
| 8 | Upload CSV, map (incl. zero-vs-missing), validate, confirm, readiness updates | Phone | Handoff §8 scripts 3–5. Request shapes (multipart `file`, `Idempotency-Key`, mapping body) are covered by `ImportWizardTest`. |
| 9 | Own data gets baselines; AI shown as not yet available with the reason | Phone | Script 4: "60 of 120 days", Train disabled, baseline forecast works. |
| 10 | Honest async job states; completed results reopen from history, incl. offline | Phone + Auto | Polling schedule and job contract: `ForecastContractTest`. Cache fallback rules: `ResponseCacheTest`. Offline: open items online, switch to airplane mode, reopen from History. |
| 11 | Every number comes from the backend; metrics state window, series, model version; only where actuals exist | Phone + Auto | `ForecastPresentationTest` (pairing, verdicts, totals from raw values). Check result screens show window and model version. |
| 12 | Model info shows card: training data, horizon, backtest, limitations | Phone | Options → Model info on the demo store. |
| 13 | No secrets in app or repo; ML service not public | Auto (app side) | `AcceptanceGuardTest`: no URLs in source, only INTERNET permission, cleartext only in the debug manifest. `local.properties` is git-ignored. Tokens use Keystore-backed storage. ML service exposure is a backend/deployment matter. |

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

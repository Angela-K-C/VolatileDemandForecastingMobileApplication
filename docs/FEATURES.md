# Features by screen

Endpoints are from `SemProject_Backend/docs/api.md`. "Role" shows who can use the action. The Reference workspace is the read-only demo store (1,437 products, M5 data).

## Entry and authentication

| Screen | What it does | API |
|--------|--------------|-----|
| Splash / routing | Shows the system splash, then routes to Sign in or the Dashboard based on the stored session | – |
| Sign in | Email and password; inline checks; friendly messages for wrong credentials, rate limiting (shows the wait) and offline | `POST /v1/auth/login` |
| Register | Name, email, password with the backend's length limits; handles `REGISTRATION_CLOSED` and an already-used email | `POST /v1/auth/register` |
| Forgot password | Two stages: request a code (the backend never says whether the account exists), then enter code and new password; signs in on success | `POST /v1/auth/password-reset/request`, `…/confirm` |
| Session expiry | A rejected refresh token returns the user to Sign in with an explanatory message | `POST /v1/auth/refresh` |
| Sign out | Confirmation, then local sign-out first and server logout in the background | `POST /v1/auth/logout` |

## Workspaces and stores

| Screen | What it does | API |
|--------|--------------|-----|
| Workspace chip (top bar) | Shows the active workspace and store, with a "demo" mark for the Reference workspace; opens the picker | – |
| Workspace picker | Lists every workspace the user can see with its stores; the demo is always present and marked. Choosing a store makes it active and remembers it | `GET /v1/workspaces`, `GET /v1/workspaces/{id}/stores` |
| Create workspace | One form creating a workspace and its first store (code, name, country, optional region, time zone, currency). Defaults KE / Africa/Nairobi / KES. A failed store step does not create a second workspace on retry. Server field errors appear on the right field | `POST /v1/workspaces`, `POST /v1/workspaces/{id}/stores` |

## Main tabs

| Tab | What it does | API |
|-----|--------------|-----|
| Dashboard | Store summary: history range and counts, AI availability with the reason (or eligibility progress "60 of 120 days"), readiness, quick actions. For stores you manage: an Upload action and an onboarding message when there is no history | history summary, readiness, `models/eligibility`, capabilities |
| Products | Paged list of 1,437 (demo) or your products with search, demand-class filter (popup) and long-press context menu. Tapping opens the product | `GET /v1/stores/{sid}/products` |
| Data | History summary, Upload button (Owner/Manager of own workspaces), link to Readiness, import list with status filter and paging; long-press an import to delete (confirmed). The demo and Viewers see an explanatory note instead of Upload | `GET /v1/stores/{sid}/imports`, `DELETE /v1/imports/{id}` |
| Forecasts | **Store-wide forecast.** Pick how far ahead (from `capabilities.horizons`), tap **Forecast all products**, watch "Forecasting… 40 of 100 products", then see every product with its expected units for the period, searchable and paged. Says whether the numbers come from the AI or the simple forecast. Flags a forecast made before new sales were imported, and offers Try again for products that failed for a temporary reason. Tapping a product opens its result. Finished pages reopen offline | `POST /v1/stores/{sid}/forecast-batches`, `GET /v1/forecast-batches/{id}`, `…/items`, `GET …/forecast-batches/latest`, `POST …/retry` |

## Product and forecast flow

| Screen | What it does | API |
|--------|--------------|-----|
| Product detail | Demand class, key facts and a sales-history chart with a range choice; days without a record are gaps, not zeros. Starts a forecast (not for Viewers in own workspaces) | product, history endpoints |
| Forecast configuration | One product: choose how far ahead (from `capabilities`). Forecasts always start after the latest recorded sales day; the method is chosen by the app (AI alongside the simple forecast when the store has a model) | `GET …/models/capabilities` |
| Job progress | Honest states while polling (queued, running, done, failed); failed jobs offer Retry only when the API allows | `POST /v1/forecasts`, `GET /v1/forecast-jobs/{id}`, `POST …/retry` |
| Forecast result | **Expected sales** headline (whole units over the period) and where the number comes from, a chart of recent sales and expected sales, and a day-by-day list. **How accurate is this?** opens the comparison of every method day by day, this product's and the model's past accuracy, and the ~4% high note on AI days 2–7. CSV export and delete | `GET /v1/forecasts/{id}`, `…/export?format=csv` |
| AI model | Whether forecasts use the AI or the simple forecast and why, the training period, and how both did on past sales. Business stores also show the **AI for your store** training section (below) | `GET …/models/active` |

## Business journey

| Screen | What it does | API |
|--------|--------------|-----|
| Import wizard | 1 Choose file (system picker, no storage permission) · 2 Check file and preview rows, then Upload · 3 Map columns · 4 Validation (counts, rejected rows with line numbers, warnings, first stored days) · 5 Overlap policy (only if days overlap: keep or replace) · 6 Confirm · 7 Result (rows added/replaced/kept, new products, readiness, next actions). System Back steps through the wizard | `POST /v1/stores/{sid}/imports`, `PUT /v1/imports/{id}/mapping`, `GET …/validation`, `GET …/errors`, `POST …/confirm`, `DELETE` |
| Readiness | "X of Y products ready", the thresholds from the API, a stale-history notice, per-product reasons (not enough history, too many gaps, stale). States that simple forecasts work for every product | `GET /v1/stores/{sid}/readiness` |
| AI for your store (in Model info) | Eligibility checks as "value of required" with Passed / Not yet. Train button for Owner/Manager once eligible. Progress while training; outcomes `TRAINED`, `GATE_FAILED` (plain wording with metrics, baseline stays in use), `NOT_ELIGIBLE`, `FAILED` (Retry only if `retryable`) | `GET …/models/eligibility`, `POST …/models/train`, `GET /v1/training-runs/{id}`, `POST …/retry` |

## Team and settings

| Screen | What it does | API |
|--------|--------------|-----|
| Team | Members with roles. Owners: invite by email with a role, change role, remove members, revoke pending invitations. Anyone can leave a workspace. Handles `LAST_OWNER`, `ALREADY_MEMBER`, `INVITATION_NOT_PENDING`. The demo workspace shows "no team" | members and invitations endpoints |
| My invitations | Accept or decline pending invitations; accepting reloads the profile and workspace list. A badge on Settings counts pending ones | `GET /v1/me/invitations`, accept, decline |
| Settings | Profile, current workspace/role/store with time zone and currency, theme (follow phone, light, dark), number display (one decimal or whole units), links to Change password, My invitations, workspace switcher, Help, app version | `GET /v1/me`, `GET /v1/stores/{sid}` |
| Change password | Current + new + confirm; wrong current password shown on that field; stays signed in on success (other sessions are ended by the backend) | `POST /v1/auth/change-password` |
| Help | Plain explanations of how forecasts work, when AI is available, limits (including the "lower error" caveat and ~4% high note), and what happens to your data | – |

## Removed from the business version (kept on the `research` branch)

Research summary and export, model-card details (inputs, accuracy by day ahead, limitations list), model-version choice, and replays on past as-of dates. The table below describes the `research` branch.

## Research (research branch only)

| Screen | What it does | API |
|--------|--------------|-----|
| Research summary | Appears in the Options menu only when `isResearch`. Compares AI, "same as last day" and "same weekday last week" on exactly the same days: overall, by demand type, by day ahead. Optional as-of date range. Shows the API's note | `GET /v1/research/summary` |
| Research export | Saves the full research CSV for the current range through the system file dialog | `GET /v1/research/export` |

## Offline behaviour

See `ARCHITECTURE.md` (Offline cache). Viewable offline: products, imports, forecast history and completed forecasts, with a "saved copy from <time>" notice. Needing a connection: uploading, validation, readiness, training and forecast generation.

## Rules applied everywhere

- Every number comes from the API; the UI is driven by `capabilities`.
- `units: null` is drawn as a gap, never 0.
- One decimal by default, with a whole-units option; totals use raw values.
- Metrics blocks pair AI and baseline from the same block and carry the "lower error" caveat.
- Roles: Viewers cannot generate (own workspace), upload or train; only Owners manage the team.

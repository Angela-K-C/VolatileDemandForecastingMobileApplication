# Retail Demand Forecasting Mobile

An Android app for retail business owners: upload your sales history, then get a forecast of how many units of every product you are likely to sell over the next day or week.

Each product gets **one number**. It comes from the shop's own AI model only when that model beat the simple forecast ("same as the last day") on the shop's past sales; otherwise it comes from the simple forecast, and the screen says which. The full comparison of methods is one tap away under **How accurate is this?**

> **Branches.** `main` is this business version. The earlier research version (AI vs. baselines side by side, replays on past dates, model-version choice and the research summary) is kept on the `research` branch.

The app contains **no forecasting maths**. Every number on screen comes from the backend API.

New to the project or to forecasting? Start with [`docs/HOW_FORECASTING_WORKS.md`](docs/HOW_FORECASTING_WORKS.md): how demand forecasting is usually done, how this project does each part, and what role the trained Walmart model plays.

## The project in one story

**1. The question.** Can machine learning predict how many units a shop will sell better than simple rules of thumb, such as "tomorrow sells the same as today"? And does that still hold for products whose demand jumps around, the ones that sell 0, 0, 3, 0, 1 units over a few days?

**2. The experiment.** We used Walmart's real sales for one store (the public M5 dataset: store CA_1, 1,437 food products, 2011–2016).
- Each day of each product was turned into inputs the model can learn from: what sold yesterday, a week ago, two and three weeks ago, the recent average, the weekday, the month, the price.
- A LightGBM model was trained on everything except the last four weeks.
- It was then tested on those last four weeks, which it had never seen.

It missed by about 26% less than "same as last day" (a typical miss of 2.38 units against 3.22). It won for every kind of product, including ones that sell only now and then. That is the research result.

**3. The insight.** The trained model has learned *Walmart*: its product codes, its calendar, its US holidays and its dollar prices. It can't be pointed at another shop's products. But we also tested the *method* with every Walmart-specific input removed, keeping only recent sales, the calendar, local public holidays and the shop's own prices. Accuracy dropped by just 0.1%. So the valuable thing we built is the **recipe**: the inputs, the training method and the honest test against a simple forecast. The trained Walmart model is the evidence that the recipe works.

**4. The product.** The app gives that recipe to any shop:

| When | What the shop owner gets |
|---|---|
| **Day one** | Upload past sales (a CSV of date, product, units sold, optionally price). Every product with sales history gets the **simple forecast** straight away (the units from its most recent recorded day, repeated for each day ahead). It is a fair start, because it is exactly what the AI has to beat. |
| **About four months of history** | The shop can train **its own model** with the same recipe, tested on its own last four weeks. |
| **If it wins** | The app switches to the shop's AI model for its forecasts and says so. |
| **If it doesn't** | It keeps the simple forecast and explains why. No model is used just because it is "AI". |
| **After that** | The model is retrained every week as new sales arrive, and has to win again each time. |

Each product shows **one number**, the units expected tomorrow or over the next 7 days. The comparison with other methods is one tap away under **How accurate is this?**

**5. Where the Walmart model lives now.** It is the proof behind the project, and it powers the **sample shop**: a ready-made shop with Walmart's real history that anyone can explore before uploading their own data.

**In one sentence:** we didn't just build a forecast for Walmart; we built and proved a *way of making forecasts*, and the app hands that to each shop once it has enough of its own data.

More detail: [`docs/HOW_FORECASTING_WORKS.md`](docs/HOW_FORECASTING_WORKS.md) for the whole project, and `docs/how-the-backend-works.md` in the backend repository for the server side.

## Project layout (three repositories)

| Repository | Role |
|------------|------|
| `SemProject_MLEngine` | Model research and the ML service. Holds the app handoff (`MOBILE_APP_HANDOFF.md`), the design spec (`retail_demand_forecasting_mobile_design.md`) and sample CSVs. Not modified by this app's work. |
| `SemProject_Backend` | FastAPI backend (Docker). Contract: `docs/api.md`. The business version needs its store-wide forecast endpoints (migration `0005`), merged into its `main`; that change was approved by the project owner. At the owner's request, team management and invitations were later removed from it (migration `0006`). |
| `VolatileDemandForecastingMobileApplication` | This repository: the Android app. |

## Status

All eight steps of the handoff's build order are implemented and pushed to `main`:

1. Foundation  2. Authentication  3. Demo journey  4. Business journey  5. Team and settings  6. Research  7. Offline cache  8. Tests and acceptance

After the eight steps, `main` was reworked for business owners (see "Business version" in `docs/BUILD_LOG.md`). On 23 November 2026 the step 5 team features (Team screen, invitations) were removed at the owner's request, so each shop belongs to the account that created it. Automatic checks pass (`./gradlew lintDebug testDebugUnitTest`: 77 unit tests, lint with 0 errors). The signed-in screens have **not** been exercised by automated tooling; they were checked by hand on a physical phone up to step 3, and steps 4–8 still need a manual pass. `docs/ACCEPTANCE.md` lists exactly what is proved automatically and what needs a person.

## Documentation

| File | Contents |
|------|----------|
| `SETUP.md` | **Start here as a new collaborator:** full setup of all three repositories, from cloning and the M5 data to the app running |
| `README.md` | This overview, a short setup, how to run and test |
| `docs/HOW_FORECASTING_WORKS.md` | How demand forecasting is usually done, how this project does each part, and the role of the trained model |
| `docs/ARCHITECTURE.md` | Stack, layering, packages, patterns, networking, auth, caching, UI conventions |
| `docs/FEATURES.md` | Every screen: what it does, the API it uses, and the rules it follows |
| `docs/BUILD_LOG.md` | What was built in each step, the decisions made, problems hit and how they were fixed |
| `docs/ACCEPTANCE.md` | Spec §19 checklist: automatic evidence vs. manual checks, known gaps |
| `docs/demo/DEMO_GUIDE.md` | The sample sales file and a step-by-step plan for demonstrating the app |
| `CLAUDE.md` | Working rules for the AI assistant used on this project |

## Requirements

- Android Studio with JDK 17+ and the Android SDK. `minSdk 26`, `targetSdk 37`.
- The backend running locally (Docker) and seeded with the sample shop (`SETUP.md` does both).
- An Android phone or emulator.

## Setup

New to the project? Follow **[SETUP.md](SETUP.md)**: it covers all three repositories, the M5 data download and the Windows setup script. The short version, for when the backend is already set up:

1. Start the backend (in `../SemProject_Backend`): `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d`. It serves `http://localhost:8000`.
2. Create `local.properties` in this repo's root (it is git-ignored) with the SDK path and the API address:
   ```
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   api.baseUrl=http://localhost:8000
   ```
   - **Physical phone over USB:** keep `localhost` and run `adb reverse tcp:8000 tcp:8000` (again after every re-plug).
   - **Emulator:** use `http://10.0.2.2:8000` (this is also the default if `api.baseUrl` is missing).
3. Build and install: `./gradlew installDebug`.
4. Go through the intro tour, then create an account in the app (or sign in with one that exists on your backend). Password-reset codes appear in `docker compose logs api` (run in `../SemProject_Backend`).

Plain HTTP is allowed in **debug builds only** (`app/src/debug/AndroidManifest.xml`). The release manifest is HTTPS-only.

## Run the checks

```
./gradlew testDebugUnitTest     # 77 unit tests
./gradlew lintDebug             # Android lint
```

## Sample data for the business journey

**For demos, use the built-in sample file:** Data → Upload sales data → **Use the sample sales file**. It is a made-up Kenyan mini-market (26 products, 300 days, ending yesterday) with enough history for the shop's own AI model. Set the shop up as Kenya / KES / Africa/Nairobi. [`docs/demo/DEMO_GUIDE.md`](docs/demo/DEMO_GUIDE.md) describes the file and gives a step-by-step demo plan.

For testing particular cases, `../SemProject_MLEngine/data/processed/samples/` has three CSVs cut from a real Walmart store (M5 TX_1), with dates shifted so history ends yesterday:

| File | Wizard answers | Expected |
|------|----------------|----------|
| `sample_shop_eligible.csv` | zero-sales question: **Yes** | Needs Attention (real spikes), confirm; eligible; Train gives `TRAINED`, horizons 1 and 7, in about 25 s |
| `sample_shop_small.csv` | **No** | "60 of 120 days", Train refused (`NOT_ELIGIBLE`), baselines work |
| `sample_shop_messy.csv` | **Yes**, date format `DD/MM/YYYY` | 5 rejected rows with line numbers, plus warnings |

Create the workspace and store as US / TX / USD / America/Chicago. Push the files to the phone with `adb push <file> /sdcard/Download/`.

## Rules this project follows

- No XML screen layouts. Every screen is Jetpack Compose inside a Fragment.
- No forecasting maths in the app; the UI is built from the API's `capabilities`, never hardcoded horizons, dates or thresholds.
- Honest results: show when the baseline beat the AI, and show `GATE_FAILED` plainly.
- `units: null` means "not recorded" and is drawn as a gap, never as 0.
- Every metrics block is paired (AI and baseline from the same API object) and carries: "Lower error means it did better on that sample, not that it will do better tomorrow."
- The roughly 4% high bias on days 2–7 of the 7-day forecast is always stated.
- The sample shop (the backend's Reference workspace) is read-only.
- No secrets in the repo. Tokens are stored encrypted with the Android Keystore.
- Never commit attribution trailers (see `CLAUDE.md`).

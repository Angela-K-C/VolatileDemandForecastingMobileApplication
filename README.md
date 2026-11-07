# Retail Demand Forecasting Mobile

An Android app for next-day and 7-day demand forecasts for retail products. It is the mobile client of a research project on forecasting under volatile demand: every forecast shows the AI model next to two simple baselines, and the app reports plainly when the AI does **not** win.

The app contains **no forecasting maths**. Every number on screen comes from the backend API.

## Project layout (three repositories)

| Repository | Role |
|------------|------|
| `SemProject_MLEngine` | Model research and the ML service. Holds the app handoff (`MOBILE_APP_HANDOFF.md`), the design spec (`retail_demand_forecasting_mobile_design.md`) and sample CSVs. Not modified by this app's work. |
| `SemProject_Backend` | FastAPI backend (Docker). Contract: `docs/api.md`. Not modified by this app's work. |
| `VolatileDemandForecastingMobileApplication` | This repository: the Android app. |

## Status

All eight steps of the handoff's build order are implemented and pushed to `main`:

1. Foundation  2. Authentication  3. Demo journey  4. Business journey  5. Team and settings  6. Research  7. Offline cache  8. Tests and acceptance

Automatic checks pass (`./gradlew lintDebug testDebugUnitTest`: 64 unit tests, lint with 0 errors). The signed-in screens have **not** been exercised by automated tooling; they were checked by hand on a physical phone up to step 3, and steps 4–8 still need a manual pass. `docs/ACCEPTANCE.md` lists exactly what is proved automatically and what needs a person.

## Documentation

| File | Contents |
|------|----------|
| `README.md` | This overview, setup, how to run and test |
| `docs/ARCHITECTURE.md` | Stack, layering, packages, patterns, networking, auth, caching, UI conventions |
| `docs/FEATURES.md` | Every screen: what it does, the API it uses, and the rules it follows |
| `docs/BUILD_LOG.md` | What was built in each step, the decisions made, problems hit and how they were fixed |
| `docs/ACCEPTANCE.md` | Spec §19 checklist: automatic evidence vs. manual checks, known gaps |
| `CLAUDE.md` | Working rules for the AI assistant used on this project |

## Requirements

- Android Studio with JDK 17+ and the Android SDK. `minSdk 26`, `targetSdk 37`.
- The backend running locally (Docker) and seeded with the demo store.
- An Android phone or emulator.

## Setup

1. Start the backend (in `../SemProject_Backend`): `docker compose up`. It serves `http://localhost:8000`.
2. Create `local.properties` in this repo's root (it is git-ignored) with the SDK path and the API address:
   ```
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   api.baseUrl=http://localhost:8000
   ```
   - **Physical phone over USB:** keep `localhost` and run `adb reverse tcp:8000 tcp:8000` (again after every re-plug).
   - **Emulator:** use `http://10.0.2.2:8000` (this is also the default if `api.baseUrl` is missing).
3. Build and install: `./gradlew installDebug`.
4. Sign in with an account that exists on your backend. Password-reset codes and other emails appear in `docker compose logs api` (run in `../SemProject_Backend`).

Plain HTTP is allowed in **debug builds only** (`app/src/debug/AndroidManifest.xml`). The release manifest is HTTPS-only.

## Run the checks

```
./gradlew testDebugUnitTest     # 64 unit tests
./gradlew lintDebug             # Android lint
```

## Sample data for the business journey

`../SemProject_MLEngine/data/processed/samples/` has three CSVs cut from a real Walmart store (M5 TX_1), with dates shifted so history ends yesterday:

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
- The demo (Reference) workspace is read-only.
- No secrets in the repo. Tokens are stored encrypted with the Android Keystore.
- Never commit attribution trailers (see `CLAUDE.md`).

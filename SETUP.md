# Setting up the project

This walks a new collaborator from an empty machine to the Android app running against a local backend, with the sample shop loaded. The fast path is Windows with one setup script; a manual path for macOS and Linux is near the end.

Expect about **45 minutes** the first time, most of it downloads, Docker builds and loading the sample shop.

## 1. What you are setting up

The project is three Git repositories that must sit **side by side in one folder**:

```text
SemProject\                                   (any parent folder name)
├── SemProject_MLEngine\                      model research + the private ML service
├── SemProject_Backend\                       FastAPI backend the app talks to
└── VolatileDemandForecastingMobileApplication\   this Android app
```

```text
Android app ──HTTP(S)──▶ API (FastAPI) ──┬──▶ PostgreSQL
                            ▲            │
                worker ─────┘            └──▶ ML service (mlengine, Docker-internal only)
```

| Repository | What it is | Key documents |
|------------|------------|---------------|
| [SemProject_MLEngine](https://github.com/Karanei-Kimutai/SemProject_MLEngine) | M5 demand-forecasting research (notebooks), the `mlengine` Python package, the trained models (committed in `models/`) and the ML service. Also exports the sample shop's data and the Walmart sample CSVs | `README.md`, `docs/README.md` (index), `docs/operations.md`, `MOBILE_APP_HANDOFF.md`, `retail_demand_forecasting_mobile_design.md` (the original app spec) |
| [SemProject_Backend](https://github.com/Karanei-Kimutai/SemProject_Backend) | Python 3.12 FastAPI app with PostgreSQL, a job worker, and Docker Compose. Owns users, shops, sales history, imports, forecast jobs (one product or the whole shop) and training jobs. Includes the Windows setup script | `README.md`, `docs/how-the-backend-works.md` (start here), `docs/api.md` (the app's API contract), `docs/architecture.md`, `docs/deployment.md` |
| [VolatileDemandForecastingMobileApplication](https://github.com/Karanei-Kimutai/VolatileDemandForecastingMobileApplication) | Kotlin + Jetpack Compose Android app | `README.md`, `docs/HOW_FORECASTING_WORKS.md` (the whole project explained), `docs/ARCHITECTURE.md`, `docs/FEATURES.md`, `docs/BUILD_LOG.md`, `docs/ACCEPTANCE.md`, `docs/demo/DEMO_GUIDE.md` |

In Docker the stack runs four containers: `db` (Postgres 16), `ml` (built from `SemProject_MLEngine`), `api` and `worker` (built from `SemProject_Backend`). Only the API is published to your machine (port 8000), plus Postgres on `127.0.0.1:55433` for inspection; the ML service is reachable only from inside Docker.

### Branches

Use `main` in all three repositories. In the app:

| Branch | What it is |
|--------|-----------|
| `main` | The app for retail business owners: create a shop, upload sales, forecast every product, train the shop's own AI model. Includes the intro tour, the getting-started checklist and a built-in sample sales file |
| `research` | The earlier research version: AI beside both baselines, replays on past dates in the sample shop, model-version choice, and the research summary and export (for research users) |

## 2. Install the prerequisites

| Tool | Notes |
|------|-------|
| **Git** | Any recent version |
| **Docker Desktop** | Use the WSL 2 engine. Give it at least 4 GB of memory (Settings → Resources) |
| **Python 3.12** | 3.10 or newer works; 3.12 matches the Docker images. On Windows install from python.org and keep the `py` launcher |
| **Android Studio** | Recent stable version. It brings the Android SDK, `adb` and the JDK that Gradle needs. Install an emulator image, or use a phone |
| **A Kaggle account** | For the M5 dataset (free) |

Disk space: about 0.5 GB for the M5 data, 0.1 GB for the exports, and a few GB for Docker images.

Optional: an Android phone with **Developer options → USB debugging** turned on.

## 3. Clone the repositories

```powershell
mkdir SemProject; cd SemProject
git clone https://github.com/Karanei-Kimutai/SemProject_MLEngine.git
git clone https://github.com/Karanei-Kimutai/SemProject_Backend.git
git clone https://github.com/Karanei-Kimutai/VolatileDemandForecastingMobileApplication.git
```

Keep the folder names exactly as above: the backend's Docker Compose file builds the ML service from `../SemProject_MLEngine`, and the setup script looks for the app at `../VolatileDemandForecastingMobileApplication`.

## 4. Download the M5 data

The raw data is not in Git (it is large and under Kaggle's competition terms).

1. Open the [M5 Forecasting – Accuracy data page](https://www.kaggle.com/competitions/m5-forecasting-accuracy/data), sign in, and accept the competition rules (downloads fail until you do).
2. Download the data and unzip it.
3. Put these files in `SemProject_MLEngine\data\raw\` (create the folders; the names must not change):

```text
SemProject_MLEngine\data\raw\
├── calendar.csv                  required
├── sales_train_evaluation.csv    required
├── sell_prices.csv               required
└── sales_train_validation.csv    only needed for the research notebooks
```

With the Kaggle CLI instead (after putting your API token in `%USERPROFILE%\.kaggle\kaggle.json`):

```powershell
pip install kaggle
kaggle competitions download -c m5-forecasting-accuracy -p SemProject_MLEngine\data\raw
# then unzip m5-forecasting-accuracy.zip into that same folder
```

From this data the setup builds the **sample shop** (called the demo store or Reference workspace in the backend: Walmart store CA_1, 1,437 FOODS products, 2011–2016) and three Walmart **sample CSVs** for testing imports.

## 5. Run the setup script (Windows)

Start Docker Desktop and wait until it says *Engine running*. Then, in PowerShell:

```powershell
cd SemProject\SemProject_Backend
powershell -ExecutionPolicy Bypass -File .\scripts\setup-local.ps1
```

It asks for an optional **research user** email. Leave it blank for the business app on `main`: you register an account in the app instead. Only the `research` branch uses a research user (step 8); to create one without being asked, add `-ResearchEmail you@example.com` and choose its password when prompted (at least 10 characters; typing is hidden).

The script is safe to re-run: it skips whatever is already done. In order, it:

1. **Checks** Docker, Git, Python, the three folders and the M5 data, and stops with a clear message if something is missing. It also converts any model file that Git checked out with Windows line endings, which LightGBM can't read (see Troubleshooting).
2. **Writes `SemProject_Backend\.env`** from `.env.example` with random local secrets (database password, JWT secret, ML service token). Postgres is exposed on `localhost:55433` and the API on `localhost:8000`. An existing `.env` is kept.
3. **Creates the ML engine's Python environment** (`SemProject_MLEngine\.venv`), installs `mlengine` with its service and test extras (`.[service,dev]`), and exports the sample shop (`data\processed\reference`) and the Walmart sample CSVs (`data\processed\samples`). The CSVs' dates are shifted so their history ends yesterday.
4. **Builds and starts the stack** with `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build`. The API applies database migrations as it starts. The script waits for `http://localhost:8000/healthz`, checks that the API can reach the ML service, and checks that the ML service can load every model (repairing line endings in the `models` volume if needed).
5. **Loads the sample shop**: 2.8 million sales rows, about 7 minutes, first run only.
6. **Creates the research user**, if you gave an email.
7. **Connects the Android app**. If they're missing, it fills in `local.properties` in the app folder:
   - `sdk.dir`, pointing at the Android SDK;
   - `api.baseUrl`: `http://localhost:8000` when a USB phone is connected, otherwise `http://10.0.2.2:8000` for the emulator.

   It then runs `adb reverse tcp:8000 tcp:8000` for each connected phone.

Options:

| Option | Effect |
|--------|--------|
| `-ResearchEmail you@example.com` | Research user to create (research branch only); leave it out to be asked (blank skips) |
| `-SkipPython` | Don't create the Python environment or re-export data (uses existing exports) |
| `-ReseedDemo` | Load the sample shop again even if it is already loaded |
| `-AppFolder <name>` | The app folder's name, if you cloned it under a different name |

When it finishes you should see `Demo store (the sample shop): 1437 products` and the API address. The interactive API docs are at <http://localhost:8000/docs>.

## 6. Build and run the Android app

1. Open `VolatileDemandForecastingMobileApplication` in Android Studio and let Gradle sync. The first sync downloads dependencies and takes a few minutes.
2. Check `local.properties` in the app folder. It is git-ignored and specific to your machine; the script fills it in, or you can write it yourself:
   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   api.baseUrl=http://10.0.2.2:8000
   ```
   | Device | `api.baseUrl` | Also needed |
   |--------|---------------|-------------|
   | Emulator | `http://10.0.2.2:8000` (also the default if the line is missing) | nothing |
   | USB phone | `http://localhost:8000` | `adb reverse tcp:8000 tcp:8000` after **every** re-plug |

   Change it and rebuild when you switch between emulator and phone.
3. Pick the device and press **Run**, or from a terminal:
   ```powershell
   cd VolatileDemandForecastingMobileApplication
   .\gradlew.bat installDebug
   ```
4. Check the device can reach the API: open `http://10.0.2.2:8000/healthz` in the emulator's browser, or `http://localhost:8000/healthz` on the phone after `adb reverse`.

Debug builds allow plain HTTP for this local backend; release builds are HTTPS-only.

## 7. First run: check that everything works

On `main` (the business app):

1. The **intro tour** opens on first launch (four pages; Skip works). It ends with **Create an account**; register with any email (self-registration is on locally). The tour can be shown again from Help.
2. With no shop yet you see **Set up my shop** and **Try a sample shop**. Choose **Try a sample shop** first: it opens the Walmart sample shop (read-only, marked "Sample shop: example data"). Open **Forecasts**, pick 7 days and tap **Forecast all products**. You should see "Forecasting… n of 1,437 products", then every product with its expected units.
3. Back in **Your shops**, tap **Add another shop** (or **Set up my shop**) and create a shop as **Kenya / KES / Africa/Nairobi**.
4. The Dashboard shows the **Getting started** checklist. Tap **Upload sales data**, then **Use the sample sales file**. It is built in the app (26 products, 300 days of a made-up Kenyan mini-market, ending yesterday). Answer **Yes** to "Does a missing row mean zero sales?", then confirm.
5. **Forecasts → Forecast all products.** With no AI model yet, every number is the **simple forecast** (the last recorded day's units, repeated), and the screen says so.
6. The checklist's AI step says the shop has enough sales. Tap **Set up AI forecasts** (or menu → **AI model**), then **Train AI model**. Training takes a few minutes. The model is used only if it beats the simple forecast on the shop's own last weeks; with this file it does. Forecast again to see AI numbers.

[`docs/demo/DEMO_GUIDE.md`](docs/demo/DEMO_GUIDE.md) has the full demonstration plan. For particular cases (a shop that isn't eligible yet, a messy file with validation errors), push the Walmart sample CSVs to the device:

```powershell
adb push SemProject_MLEngine\data\processed\samples\sample_shop_eligible.csv /sdcard/Download/
adb push SemProject_MLEngine\data\processed\samples\sample_shop_small.csv /sdcard/Download/
adb push SemProject_MLEngine\data\processed\samples\sample_shop_messy.csv /sdcard/Download/
```

The wizard answers for each file are in the app's `README.md` and `SemProject_MLEngine\data\processed\samples\README.md`. Create a store for them as US / TX / USD / America/Chicago.

On the `research` branch, sign in with a research user instead, choose the sample shop and try a replay: Products → `FOODS_3_090` → forecast with as-of **2016-05-08**, next day, Compare. Expected: AI ≈ 57.1, "same as last day" 77, actual 72.

## 8. Accounts and email

- **Normal users** register in the app. Or create one from the command line:
  ```powershell
  cd SemProject_Backend
  docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm api python -m app.cli create-user someone@example.com
  ```
  Running it again for an existing email sets a new password.
- **Research users** (add `--research` to the command above) matter only on the app's `research` branch, where they can choose model versions and past as-of dates and see the research summary and export. On `main` they behave like normal users.
- **Roles:** whoever creates a shop is its Owner. There are no invitations, so each shop belongs to one account.
- **Email** (password-reset codes) is not sent locally. It is printed in the API log:
  ```powershell
  docker compose -f docker-compose.yml -f docker-compose.dev.yml logs api
  ```
- Never commit `SemProject_Backend\.env` or `local.properties`. Both are git-ignored.

## 9. Everyday commands

Run these in `SemProject_Backend`. `$dc` is shorthand for the two Compose files:

```powershell
$dc = @("compose", "-f", "docker-compose.yml", "-f", "docker-compose.dev.yml")
docker @dc up -d            # start (data is kept)
docker @dc down             # stop (data is kept)
docker @dc ps               # status
docker @dc logs -f api      # API log, including emails
docker @dc logs -f worker   # forecast and training job worker
docker @dc logs -f ml       # ML service
docker @dc up -d --build    # rebuild after pulling backend or ML engine changes
docker @dc down -v          # stop and DELETE all local data (re-run the setup script afterwards)
```

After `git pull` in the backend or ML engine, rebuild with `up -d --build`; the API applies database migrations on start. Re-running the setup script does the same and checks everything.

**Fresh sample data.** The in-app sample sales file is always built to end yesterday, so it needs nothing. The Walmart sample CSVs end on the day you export them; to refresh them, run `.\scripts\setup-local.ps1` again, or in `SemProject_MLEngine`: `.\.venv\Scripts\python.exe -m mlengine export-samples`.

## 10. Running the tests

| Repository | Command | Needs |
|------------|---------|-------|
| App | `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat lintDebug` | Nothing else (79 JVM tests; lint 0 errors) |
| ML engine | `.\.venv\Scripts\python.exe -m pytest` | The setup script's environment (it installs the test tools). 23 tests in about 15 s, plus 3 full-data tests that are skipped unless `MLENGINE_FULL_TESTS=1` (needs the raw data, about 2 minutes) |
| Backend | `pytest` with `TEST_DATABASE_URL` set | Python 3.12 with `pip install -e ".[dev]"` and a PostgreSQL test database; see `SemProject_Backend\README.md`. 67 tests |

## 11. Manual setup (macOS, Linux, or without the script)

From the parent folder, after steps 2–4:

```bash
# 1. Backend settings
cd SemProject_Backend
cp .env.example .env
#   Edit .env:
#     API_DOMAIN=localhost
#     POSTGRES_PASSWORD=<random>
#     DATABASE_URL=postgresql+psycopg://forecast:<same password>@db:5432/forecast
#     JWT_SECRET=<openssl rand -hex 32>
#     ML_SERVICE_TOKEN=<openssl rand -hex 24>
#     DB_PORT=55433
#     API_PORT=8000

# 2. ML engine environment and data exports
cd ../SemProject_MLEngine
python3 -m venv .venv && source .venv/bin/activate
pip install -e ".[service,dev]"
python -m mlengine export-reference      # sample shop -> data/processed/reference
python -m mlengine export-samples        # Walmart sample CSVs -> data/processed/samples
deactivate

# 3. Start the stack and wait for http://localhost:8000/healthz
cd ../SemProject_Backend
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# 4. Load the sample shop (about 7 minutes)
docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm \
  -v "$(pwd)/../SemProject_MLEngine/data/processed/reference:/seed:ro" \
  api python -m app.cli seed-reference /seed

# 5. Optional, research branch only: a research user (you type the password)
docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm api \
  python -m app.cli create-user you@example.com --research
```

Then follow step 6, using `./gradlew installDebug`. On macOS the SDK path in `local.properties` is usually `sdk.dir=/Users/<you>/Library/Android/sdk`.

## 12. Troubleshooting

| Problem | Fix |
|---------|-----|
| "running scripts is disabled on this system" | Run the script exactly as shown, with `powershell -ExecutionPolicy Bypass -File …` |
| "Docker isn't running" | Start Docker Desktop and wait for *Engine running*, then re-run |
| "The M5 data isn't there yet" | Step 4. On Kaggle you must accept the competition rules first |
| Kaggle download gives 403 | Same: accept the rules on the competition page with the account your token belongs to |
| API doesn't come up, or Postgres authentication errors | Usually an old database volume with a different password. Run `docker compose -f docker-compose.yml -f docker-compose.dev.yml down -v` (this wipes local data), then re-run the script |
| Port 8000 or 55433 already in use | Stop the other program, or change `API_PORT` / `DB_PORT` in `.env`. If you change `API_PORT`, change `api.baseUrl` and the `adb reverse` ports to match |
| "The API can't reach the ML service" | `docker compose … logs ml`. Rebuild with `up -d --build` |
| "The ML service can't load its models", forecasts fail with `ML_SERVICE_UNAVAILABLE`, or `[LightGBM] [Fatal] Model format error, expect a tree here` | The `model.txt` files have Windows line endings, which LightGBM can't read. This happened with checkouts made before the ML engine's `.gitattributes` existed. Re-run the setup script: it converts them on disk and in the `models` volume. By hand: in `SemProject_MLEngine`, `git pull`, delete the `models` folder's `model.txt` files and run `git checkout -- models` |
| ML engine tests stop silently partway through (exit code 127 or a crash) | Same cause: the process dies loading a CRLF model. Same fix |
| Sample shop load is slow | Normal: about 7 minutes in Docker Desktop. Don't interrupt it |
| Gradle sync fails on the JDK | Android Studio → Settings → Build Tools → Gradle → Gradle JDK: use the bundled JetBrains Runtime (17 or newer) |
| "SDK location not found" | Add `sdk.dir` to `local.properties`, or open the project once in Android Studio |
| App shows "No connection" on a USB phone | Run `adb reverse tcp:8000 tcp:8000` again (needed after every re-plug) and check `api.baseUrl=http://localhost:8000` |
| App shows "No connection" on the emulator | Use `api.baseUrl=http://10.0.2.2:8000`, and check the backend is up at <http://localhost:8000/healthz> on your PC |
| `adb` not found | Use `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, or add that folder to `PATH` |
| In Git Bash, `/sdcard/...` paths turn into `C:/Program Files/Git/sdcard/...` | Run `export MSYS_NO_PATHCONV=1` first, or use PowerShell |
| "Your latest sales are from …" notice, or readiness says history is stale | Forecasts start the day after the last recorded sale, so the app asks for sales up to yesterday. Upload newer sales. For the Walmart sample CSVs, re-export them (step 9); the in-app sample file is always current, but a shop that imported it days ago needs a fresh upload (choose Replace or Keep for the overlapping days) |
| The intro tour doesn't show again | It shows once per phone. Help → **Show the app tour**, or clear the app's data |
| Password reset code never arrives | It is in `docker compose … logs api`, not in an inbox |

## 13. Before you change anything

- Read [`docs/HOW_FORECASTING_WORKS.md`](docs/HOW_FORECASTING_WORKS.md) for the whole project, then `SemProject_MLEngine\MOBILE_APP_HANDOFF.md` (rules), the API guide `SemProject_Backend\docs\api.md`, and the original app spec `SemProject_MLEngine\retail_demand_forecasting_mobile_design.md`. The spec predates the business version; where they differ, the app's `docs/FEATURES.md` describes what was built and api.md is right about the API.
- The API contract belongs to the backend. If the app needs something the API doesn't offer, raise it with the project owner rather than working around it.
- Project rules that matter for review: no XML screen layouts; no forecasting maths in the app; never fabricate data; show results honestly, including when the simple forecast wins. These are listed in the app's `README.md` and `CLAUDE.md`.
- Commit messages must not carry `Co-Authored-By` or "Generated with" lines.

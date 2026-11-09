# Project instructions

- Read ../SemProject_MLEngine/MOBILE_APP_HANDOFF.md before starting work. Then read the design spec it links to and ../SemProject_Backend/docs/api.md.
- Never add Co-Authored-By trailers or "Generated with Claude Code" lines to commits or pull requests.
- Do not modify ../SemProject_Backend or ../SemProject_MLEngine. Raise any API contract problems with the project owner.
  - Exception (approved by the project owner, 2026-11-09): store-wide forecast batches were added to the backend (built on its `forecast-batches` branch, merged into its `main`). Further backend changes still need the owner's go-ahead.
  - Exception (approved by the project owner, 2026-11-09): an experiment testing the own-data recipe on a non-Walmart dataset (UCI Online Retail II) was added to the ML engine on its `experiment-online-retail` branch. Other ML engine changes still need the owner's go-ahead.
- Branches: `main` is the app for retail business owners (upload sales, get forecasts for every product). `research` keeps the research version (AI vs baselines, replays, research summary).
- The backend runs locally in Docker at http://localhost:8000 (from the emulator: http://10.0.2.2:8000). Emails such as password reset codes appear in `docker compose logs api`, run in ../SemProject_Backend.

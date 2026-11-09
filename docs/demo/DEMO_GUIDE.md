# Demo guide

A plan for showing the app end to end (for example at the end-of-semester presentation), using the sample sales file.

## The sample sales file

`sample-shop-sales.csv` is the made-up sales history of a small Kenyan mini-market:

- **26 products** in 8 categories (dairy, bakery, staples, produce, drinks, snacks, household), priced in Kenyan shillings.
- **300 days**, one row per product per day, zeros included. 7,257 rows.
- **Columns** the app recognises without asking: `date, sku, product_name, category, units_sold, unit_price`.
- **Realistic patterns:** busier Fridays and Saturdays, month-end paydays, Kenyan public holidays, the run-up to Christmas, the rainy seasons (charcoal and umbrellas), a slow January, a 6% price rise on staples every July, week-long promotions every eight weeks, and a shop that grows about 10% a year.
- **Two new products:** Crisps (45 days of sales) and Energy drink (12 days), so the readiness screen shows a product that isn't ready yet.

It is large enough for the shop's own AI model. Run through the ML engine's real training (eligibility rules on), files ending on three different dates all passed: the AI's typical miss (RMSE) was 27–29% lower than the simple forecast's, and the 7-day forecast was validated too.

**Use the copy inside the app, not this one.** The app only treats sales as up to date if they run to yesterday. The app builds the file fresh each time, ending yesterday: Data → Upload sales data → **Use the sample sales file**. **Save a copy of the sample file** on the same screen saves that fresh copy to the phone, for example to show it in a spreadsheet app. The copy in this folder ends on 8 November 2026 and is here for reference.

To make a copy ending on another day, run this from the project root (Git Bash):

```bash
SAMPLE_SALES_OUT=docs/demo/sample-shop-sales.csv SAMPLE_SALES_END=2026-12-10 ./gradlew :app:testDebugUnitTest --tests '*SampleSalesFileTest*' --rerun
```

The numbers for a product on a given date are always the same, so copies made on different days agree where they overlap.

## Before the demo (on the day, about 30 minutes before)

1. Start the backend stack (`docker compose up -d` in `../SemProject_Backend`) and check `http://localhost:8000` responds.
2. Phone: install the latest build, connect it, and run `adb reverse tcp:8000 tcp:8000` (or use the emulator).
3. Sign in with a demo account. In the app, **set up a shop** called, for example, "Demo Mini Mart", with country Kenya, currency KES and time zone Africa/Nairobi (the AI uses the country's public holidays).
4. Upload the sample file into it (Data → Upload sales data → Use the sample sales file → follow the steps).
5. Forecast all products (Forecasts tab). For 26 products this takes a few seconds.
6. Train its AI model (dashboard checklist → Set up AI forecasts → Train). For this file it usually takes under a minute. Then forecast all products again: the numbers now come from the AI.

Doing this on the day matters: sales that end before yesterday are shown as out of date.

To show the intro tour live, clear the app's storage first (Settings → Apps → the app → Storage → Clear storage) and sign in again; or open it any time from Help → Show the app tour.

## Suggested running order (about 10 minutes)

| # | Show | Point to make |
|---|------|---------------|
| 1 | The intro tour (Help → Show the app tour) | What the app is for, and the three steps. |
| 2 | A second, empty shop ("Practice shop"): the dashboard checklist and the empty Forecasts tab | A new owner always sees one clear next step. |
| 3 | Upload the sample file into the practice shop, live | The wizard recognises the columns by itself, checks the rows and reports what it stored. |
| 4 | Forecast all products in the practice shop | Forecasts work straight away with the simple method, before any AI. |
| 5 | Switch to "Demo Mini Mart" (prepared earlier) | Its own AI model is trained and in use. |
| 6 | Forecasts tab: open a product, then "How accurate is this?" | The AI was tested on the last 4 weeks and beat the simple forecast. The "?" explains each measure. |
| 7 | Readiness (Data tab) | Energy drink isn't ready yet: only 12 days of sales. It still gets the simple forecast. |
| 8 | The sample shop (Walmart data) | Where the research started: the model trained on Walmart sales, used for the demo shop. |

## If something goes wrong

- **"Sales are out of date" notice:** the uploaded sales end before yesterday. Upload a fresh sample file (the app makes one ending yesterday). The wizard asks what to do with days that were already uploaded; either choice works, and the new days are added.
- **Training is unavailable:** check that the ML service container is running (`docker compose ps` in the backend folder).
- **The phone can't reach the backend:** run `adb reverse tcp:8000 tcp:8000` again after reconnecting the cable.

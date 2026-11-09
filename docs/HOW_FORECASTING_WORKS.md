# How demand forecasting works, and how this project does it

This document explains, in plain language, how a demand forecasting product is usually built, and then how this project (the ML engine, the backend and this app) does each part. It is written for someone new to machine learning who needs to understand, explain or extend the project.

Contents:

1. [The short version](#1-the-short-version)
2. [How demand forecasting is usually done](#2-how-demand-forecasting-is-usually-done)
3. [How this project does it](#3-how-this-project-does-it)
4. [The role of the model we trained](#4-the-role-of-the-model-we-trained)
5. [What happens when a shop owner uses the app](#5-what-happens-when-a-shop-owner-uses-the-app)
6. [Where we differ from larger systems](#6-where-we-differ-from-larger-systems)
7. [Glossary](#7-glossary)

---

## 1. The short version

- **The goal.** A retail business owner uploads past daily sales and gets, for each product, the number of units they are likely to sell tomorrow or over the next 7 days.
- **The research.** We trained a LightGBM model on real Walmart sales (the M5 dataset: store CA_1, 1,437 food products, 2011–2016). On the last 28 days of that data, which the model never trained on, it missed by less than the simple forecast "tomorrow sells the same as today": a typical miss (RMSE) of 2.38 units against 3.22. It won for every kind of product, including ones that sell only on some days.
- **What the product uses.** A trained model only knows the products it learned from. A shop's products, prices and calendar are different from Walmart's, so a shop can't use the Walmart model's learned weights. What carries over is the *recipe*: the same inputs, the same training method and the same accuracy check. Each shop gets its own model, trained on its own sales with that recipe.
- **Until there is enough data.** A shop needs about four months of history before its own model can be trained and checked fairly. Until then, and whenever its model does not beat the simple forecast, the app uses the simple forecast and says so.
- **The Walmart model's job.** It powers the "sample shop" in the app and is the evidence for the research question: does machine learning beat simple forecasts when demand is volatile?

This approach, shipping a training recipe with a simple fallback and an accuracy check, is a common and defensible way to build a forecasting product. Section 6 describes what larger systems add on top.

---

## 2. How demand forecasting is usually done

### 2.1 The problem

Demand forecasting predicts how many units of each product will sell on each future day. The data is a **time series**: for each product, one number per day (units sold), often with extras such as price, promotions and holidays.

Retail data is hard for three reasons:

- **Many products sell rarely.** A typical grocery item might sell 0, 0, 2, 0, 1, 0, 0 units over a week. This is called **intermittent demand**. Averages are misleading and a forecast of "0.4 units" is normal.
- **Demand is volatile.** Promotions, holidays, weather and paydays cause spikes.
- **Every shop is different.** Different products, prices, customers and seasons.

### 2.2 Start with simple baselines

Before any machine learning, you set up **baselines**: very simple forecasts that need no training.

| Baseline | Rule |
|---|---|
| Naive ("same as last day") | Tomorrow will sell what today sold |
| Seasonal naive ("same weekday last week") | Next Monday will sell what last Monday sold |
| Moving average | The average of the last N days |
| Statistical methods | Exponential smoothing, ARIMA, Croston's method (for intermittent items) |

Baselines matter because they are surprisingly hard to beat on retail data. A machine learning model is only worth using if it beats them on the same days. Reporting a model's accuracy without a baseline next to it says very little.

### 2.3 Turn history into inputs (features)

Machine learning models such as LightGBM don't read a time series directly. You turn the history into a table, one row per product per day, with columns the model can learn from. These columns are called **features**:

- **Lags:** sales 1, 7, 14 or 21 days earlier.
- **Rolling statistics:** the average and spread of the last 3, 7 or 14 days.
- **Calendar:** weekday, month, day of the month, weekend, holidays.
- **Price:** today's price and how it changed.
- **Identity:** which product, category or store the row belongs to.

The model learns how these inputs relate to the next day's sales. Choosing good features usually matters more than choosing the model.

### 2.4 Split by time, never at random

To know whether a model works, you test it on data it has never seen. With time series, you must split **by time**: train on the past, test on the most recent days. A random split would let the model "see the future" (for example, train on Wednesday and Friday and then be tested on Thursday). This is called **leakage**, and it makes a model look far better than it is.

The usual split is three windows:

| Window | Used for |
|---|---|
| Training | The model learns from these days |
| Validation | Tuning: when to stop training, which settings to use |
| Test (backtest) | The final, honest score, on days the model never touched |

A **backtest** replays the past: make forecasts as if standing on an earlier date, then compare them with what really sold.

### 2.5 Measure errors in a few ways

| Metric | Meaning |
|---|---|
| MAE (mean absolute error) | The average size of a miss, in units |
| RMSE (root mean squared error) | Like MAE but punishes big misses more |
| WAPE (weighted absolute percentage error) | Total misses as a share of total sales |

No single number tells the whole story. On intermittent items, a model that predicts "0.4 units" every day can have a better RMSE but a worse MAE than one that predicts 0, because MAE rewards guessing the typical day (often zero). Serious projects report more than one metric and break them down by product type.

### 2.6 Forecasting more than one day ahead

To forecast 7 days, there are two usual approaches:

- **Recursive:** forecast tomorrow, feed that forecast back in as if it were real, forecast the day after, and so on. One model, but errors can build up.
- **Direct:** train a separate model (or input) for each day ahead.

Either way, you check accuracy for each day ahead, because day 7 is usually less accurate than day 1.

### 2.7 Ship the recipe, not just the weights

A trained model is a set of learned numbers (**weights**, or for LightGBM, a set of decision trees). Those numbers only make sense for the data they came from: the same products, price ranges and calendar. When every customer sells different things, products usually ship the **training pipeline**: the code that cleans data, builds features, trains, tests and decides whether to use the result. Each customer gets a model trained on their own data.

### 2.8 The cold-start problem

A new customer has little or no history. This is the **cold start**. The usual answers:

1. Use baselines or statistical methods first. They need only a few weeks of data.
2. Switch to machine learning once there is enough history to train *and* test it fairly.
3. In larger systems, use a model trained on many other shops (see section 6).

### 2.9 Gate, monitor and retrain

Before a model is used, it should pass a **gate**: an automatic check that it beats the baseline on the test window. After that, real systems keep comparing forecasts with actual sales (**monitoring**) and **retrain** as new data arrives, because shops, products and customer habits change.

### 2.10 Show the forecast honestly

A business owner needs a number they can act on ("order about 84 units"), plus a way to judge how much to trust it. Good tools show a simple headline, a way to drill into accuracy, and clear labels when the AI is not being used.

---

## 3. How this project does it

The project has three repositories:

| Repository | Role |
|---|---|
| `SemProject_MLEngine` | The research notebooks, the `mlengine` Python package (features, training, evaluation), the trained Walmart model files and an internal ML service that makes forecasts |
| `SemProject_Backend` | The API the app talks to: accounts, workspaces and stores, sales imports, forecast jobs, training jobs and team management. Stores all business data in Postgres and calls the ML service |
| `VolatileDemandForecastingMobileApplication` | This Android app |

Below, each step of section 2 is mapped to what the project does. How the backend itself works (accounts, imports, background jobs, training jobs) is explained in the backend repository's `docs/how-the-backend-works.md`.

### 3.1 The data and the problem (section 2.1)

- **Research data:** the Kaggle M5 dataset (Walmart), narrowed to one store (CA_1) and one category (FOODS): 1,437 products and 1,941 days, ending 2016-05-22.
- **Volatility:** 925 of the 1,437 products are intermittent (they sell on some days only). Each product is labelled with a demand class (smooth, erratic, intermittent or lumpy) using the Syntetos–Boylan method, and the app shows it in plain words ("Sells on some days only").
- **Business data:** a shop uploads a CSV of daily sales per product (date, product, units, optionally price and category) through the app's import wizard. Days with no row are treated as **not recorded**, which is different from zero sales, unless the owner says missing days mean zero.

### 3.2 Baselines (section 2.2)

Two baselines are computed for every forecast, for every product, with no training:

- **Same as last day** (`naive_last_value`): the main baseline, and the **simple forecast** the app shows whenever it doesn't use the AI.
- **Same weekday last week** (`seasonal_naive_7`): shown only for comparison.

#### What exactly is the simple forecast?

**The rule:** take the units the product sold on its **most recent recorded day**, up to the shop's latest day of sales, and expect that same number on **every** day of the forecast.

| | Sun 8 Nov (latest recorded day) | Mon 9 | Tue 10 | Wed 11 | Thu 12 | Fri 13 | Sat 14 | Sun 15 | 7-day total |
|---|---|---|---|---|---|---|---|---|---|
| Fresh milk: sold | 12 | | | | | | | | |
| Simple forecast | | 12 | 12 | 12 | 12 | 12 | 12 | 12 | 84 |

The details:

- **"Recorded" means the shop's data has a row for that product on that day**, including a recorded zero, or a zero filled in because the owner said missing days mean zero. Days without a row (for example, the shop was closed) are skipped. If the shop was closed yesterday, the forecast uses the last day it traded.
- **A zero carries over.** If the product sold nothing on its latest recorded day, the simple forecast is 0 for every day.
- **No minimum history.** One recorded day in the past year is enough. If a product has no recorded day in the 365 days the backend sends, its forecast fails with "no sales history". (The AI is stricter: 21 days, with at most 3 unrecorded in the last 21.)
- **Lengths:** 1 or 7 days, the same as the AI.
- **Where it is calculated:** in the ML service (`mlengine/models/baselines.py` in the ML engine), on every forecast request, including ones where the AI also runs, so the two can always be compared. The app does no forecasting maths itself; it reads the value from the API as `points[].baseline`.
- **In accuracy tests and the gate**, "same as last day" means the previous calendar day (`sales_lag_1`), and days whose previous day wasn't recorded are left out of the scoring. For a shop with no gaps, this is the same thing.

**When the app shows it** instead of the AI:

1. The shop has no AI model yet: not enough sales, not trained yet, or its model didn't beat the simple forecast at the gate.
2. The AI has no value for that product: fewer than 21 recorded days, or more than 3 unrecorded days in the last 21. The rest of the shop's products can still use the AI.
3. The model's test results don't show the AI ahead. For business shops this can't happen, because the gate only keeps models that beat the simple forecast.

**Why it is used:** it is the standard benchmark in forecasting, it needs no training, anyone can check it by hand, and for the next day it is surprisingly hard to beat. **What it misses:** weekly patterns, paydays, holidays, trends and promotions. One unusual day (a big order, a quiet holiday) is repeated across the whole period, and a 7-day total is simply seven times one day. These are exactly what the AI learns, and why it is used once it proves better.

**Same weekday last week**, the second baseline: each forecast day gets the units sold on the same weekday in the 7 days before the forecast starts (for longer periods, that week repeats). If that day wasn't recorded, it has no value. The app shows it only in the "Every method, day by day" comparison, never as the main number.

### 3.3 Features (section 2.3)

The research went through several stages (see the ML engine's `README.md`). The biggest single improvement came from using **recent** sales as inputs. The model that shipped ("Stage 3") uses:

- sales 1, 7, 14 and 21 days earlier;
- the average and spread of the last 3, 7 and 14 days;
- weekday, month, day of the month, weekend;
- price;
- identity and calendar inputs.

There are two versions of this feature set:

| Feature set | Used by | Identity and calendar inputs |
|---|---|---|
| `m5-v1` | The Walmart model | Walmart's item, department, category, store and state IDs; Walmart's day and week numbers; US events; California food-stamp (SNAP) days; Walmart prices |
| `portable-v1` | Each shop's own model | The shop's own product, category and store IDs; days since the shop's history began; the shop's country's public holidays; the shop's prices, carried forward when missing |

The two were compared on the Walmart data: `portable-v1` scored RMSE 2.380 against 2.377 for `m5-v1`, about 0.1% worse. That experiment is the evidence that the recipe transfers to other shops.

### 3.4 Model and training (sections 2.3–2.4)

- **Model:** LightGBM, a gradient-boosted decision tree model widely used for tabular and retail forecasting (it was the basis of the winning M5 solutions).
- **Objective:** Tweedie, which suits sales data with many zeros and occasional spikes.
- **Splits, measured back from the last day of history (L):**
  - training: up to L−56;
  - validation (to decide when to stop training): L−55 to L−28;
  - backtest: L−27 to L (the last 28 days).

  On the Walmart data these are exactly the M5 competition's windows.
- **Leakage rule:** a model may only make a forecast from a date if its training data ended on or before that date. The ML service enforces this. It is why the research version's "replays" (forecasts made from a past date and compared with what really sold) can be trusted.

### 3.5 Metrics (section 2.5)

Every model is scored with MAE, RMSE and WAPE against both baselines, on exactly the same days, and broken down by demand class. The Walmart model's backtest (28 days, 40,236 product-days):

| Method | RMSE | MAE | WAPE |
|---|---|---|---|
| AI (LightGBM) | 2.377 | 1.377 | 61.0% |
| Same as last day | 3.219 | 1.729 | 76.6% |
| Same weekday last week | 3.179 | 1.748 | 77.4% |

The AI beat the main baseline by about 26% on RMSE and won in every demand class. Its advantage on MAE is smaller for intermittent and lumpy products, where the baseline still wins for about a quarter of items.

### 3.6 Seven days ahead (section 2.6)

Forecasts are **recursive**: day 1 is predicted, written back into the history, then used to predict day 2, and so on. A separate backtest checked this: over 7 days the AI's RMSE was 2.47 against 3.49 for "same as last day", and error grows by only about 10% from day 1 to day 7. From day 2 onwards the forecasts run about 4% high on average; the app discloses this under "How accurate is this?".

Supported lengths: 1 day and 7 days. Longer lengths are possible in the ML service but marked experimental.

### 3.7 Recipe and per-shop models (sections 2.7–2.8)

This is the central design decision (decision 7 in the ML engine's `docs/notebook-to-production.md`): **the Walmart model is not applied to other shops' products; the recipe is retrained per shop.** The ML engine's `pipelines/own_data.py` does this:

1. **Eligibility.** A shop can train once it has:
   - at least 10 products with 21+ days of sales each;
   - at least 120 days of store history;
   - at least 4,500 product-days of recorded sales.

   These numbers come from an experiment with 48 simulated shops (10–100 products, 90–365 days). Shops with 120+ days and 4,500+ product-days reliably passed the gate; smaller ones often didn't. With fewer than about 120 days there are too few days left to train on once the 56 days for validation and testing are set aside.

   A later experiment repeated this on a shop unrelated to Walmart: a UK online gift and homeware retailer (UCI Online Retail II), whose products are mostly lumpy. The recipe beat "same as last day" by the same margin (26% lower RMSE on the whole shop), and simulated 20-product shops passed the gate in 36 of 40 runs, even with 90 days of history. So the thresholds look conservative, though two datasets are still a small sample. Absolute accuracy for lumpy products was low (the total miss was 94% of total sales), so beating the simple forecast doesn't make such forecasts precise. Details: the ML engine's `notebooks/PortableFeatures_OnlineRetail.ipynb` and §9.4 of `docs/notebook-to-production.md`.
2. **Training** with `portable-v1` features and the same splits as above.
3. **Gate** (section 2.9): the shop's model is used only if its backtest RMSE **and** MAE are both lower than "same as last day" on the same rows. Otherwise the outcome is `GATE_FAILED`, nothing is registered, and the shop keeps the simple forecast.
4. **7-day check:** a recursive backtest decides whether the shop's model is allowed 7-day forecasts or only next-day forecasts.
5. **Registration:** the model is refitted on all the shop's history and saved as `own-<workspaceId>`, with its own model card.

**Before the shop qualifies (the cold start)**, every forecast uses the baselines.

### 3.8 Monitoring and retraining (section 2.9)

- An hourly backend maintenance pass fills in actual sales on past forecasts once a shop has uploaded sales for those days ("Actuals now available").
- A forecast made before newer sales were imported is flagged as outdated, and the app offers to forecast again.
- The first training is started by an owner or manager from the app's **AI model** screen. After that, the backend retrains the shop's model weekly if new sales have been uploaded since the last training, and the gate applies again each time.

### 3.9 Showing the forecast (section 2.10)

The app (business version, `main` branch) shows **one number per product**: expected units over the chosen period. Which method that number comes from is decided by `shownMethodOf` in `data/forecast/ShownForecast.kt`:

- **AI** if the shop has a model and its test results show it beat "same as last day" (lower RMSE, or lower MAE if RMSE isn't available). Because of the gate, every registered business model qualifies.
- **Simple forecast** otherwise, or for any product where the AI has no value (for example, too many missing days).

The screen says which one is used. **How accurate is this?** opens:
- every method side by side, day by day;
- this product's own past accuracy (sample shop only);
- the model's overall test results;
- the 4% note for AI days 2–7.

The app does no forecasting maths itself: every number comes from the backend.

---

## 4. The role of the model we trained

The trained and exported Walmart model (`SemProject_MLEngine/models/m5-ca1-foods/`) has three jobs.

1. **It is the research result.** It answers the project's question: on volatile retail demand, does machine learning beat simple forecasts? Yes: about 26% lower RMSE overall, a win in every demand class, and a smaller but real advantage on intermittent items. The `research` branch of this app exists to show this, with replays, side-by-side comparisons and a research summary.
2. **It powers the sample shop.** In the app, "Try a sample shop" opens Walmart's CA_1 with its real history. Every forecast there comes from this model. A visitor can see what AI forecasts look like before uploading their own data.
3. **It proved the recipe that business shops use.** The portable-features experiment (section 3.3) showed that the same recipe, without Walmart-specific inputs, is just as accurate. That is the justification for retraining it per shop.

It is released in three versions trained on different amounts of history, so that a forecast is never made by a model that has seen the outcome:

| Version | Trained up to | Serves forecasts from |
|---|---|---|
| `1.1.0-dev` | 2016-03-27 | 2016-03-27 to 2016-04-23 |
| `1.1.0-bt` | 2016-04-24 | 2016-04-24 to 2016-05-21 (the backtest window) |
| `1.1.0` | 2016-05-22 | 2016-05-22 onwards |

What it does **not** do: forecast a business owner's own products. Its inputs include Walmart's product IDs, day numbers, US events and dollar prices, which mean nothing for another shop. The ML service rejects other products with `PRODUCT_NOT_IN_MODEL`.

---

## 5. What happens when a shop owner uses the app

1. **Sign up and sign in.** The account is created in the backend; tokens are stored encrypted on the phone.
2. **Set up a shop.** First launch offers "Set up my shop" or "Try a sample shop". Setting up creates a workspace and a store (name, country, time zone, currency). The country decides which public holidays the shop's model will use.
3. **Upload sales.** The import wizard has these steps:
   - choose a CSV;
   - match its columns (date, product, units, optional price and category);
   - review rejected rows and warnings;
   - choose what to do with days that overlap earlier uploads;
   - confirm.

   The backend stores one row per product per day.
4. **Check readiness.** The Readiness screen shows how many products have the 21 days needed to forecast, and why the others don't.
5. **Forecast.** On the Forecasts tab, choose next day or 7 days and tap **Forecast all products**. The backend queues one forecast per product (a batch). The worker sends each product's recent history to the ML service, which returns:
   - the two baselines, always;
   - the AI forecast, if the shop has a model and the product has enough clean history.

   The app polls until the batch finishes, then lists every product with its expected units.
6. **Open a product** to see the headline number, a chart of recent and expected sales, the day-by-day list, and "How accurate is this?".
7. **Grow into AI.** As uploads add history, the AI model screen shows progress ("64 of 120 days"). Once eligible, an owner or manager taps Train. If the shop's model passes the gate, all later forecasts use it; if not, the app explains that the simple forecast was better and keeps using it.

---

## 6. Where we differ from larger systems

The design above is sound and common. Larger commercial systems often add the following. They are listed so the project's limits are clear, not because they are required.

| Larger systems often… | This project | Why |
|---|---|---|
| Train one **global model** on many shops and products, so a new shop gets machine-learning forecasts from day one | Trains one model per shop; new shops use baselines until about 120 days | Simpler, easy to check per shop, and avoids applying Walmart's learned weights to unrelated products. A "starter model" trained on Walmart data with portable inputs is a possible extension; it would need testing on stores it has never seen |
| Use **pretrained forecasting models** (for example Amazon's Chronos or Google's TimesFM) that forecast new products with no training | Not used | Larger to run, harder to explain, and outside the research question |
| Use statistical methods (exponential smoothing, Croston) for cold start | Uses naive and seasonal-naive baselines | They need no fitting and are the standard comparison in the research |
| Give **ranges** ("between 60 and 100 units") | Single numbers only (`predictionIntervals: false`) | Not built in the model yet |
| Model **promotions**, stock-outs and planned price changes | Prices only; promotions and stock-outs are not inputs. A day with zero sales because the shelf was empty looks like a day with no demand | No such data in the upload format |
| Monitor accuracy continuously, with alerts when a model gets worse | Weekly retraining with the gate each time; actual sales are attached to past forecasts but not summarised for business users | Out of scope for this project |
| Forecast weeks or months ahead | 1 or 7 days | Longer lengths were not validated |

---

## 7. Glossary

| Term | Meaning |
|---|---|
| As-of date | The last day of known sales. A forecast starts the day after it |
| Backtest | Testing a model on past days it never trained on, by forecasting as if standing on an earlier date |
| Baseline | A simple forecast that needs no training, used as the bar a model must beat |
| Simple forecast | The app's name for the "same as last day" baseline: every forecast day gets the units sold on the product's most recent recorded day (section 3.2) |
| Batch | One request that forecasts every product of a store |
| Cold start | A new customer with too little history for machine learning |
| Demand class | Smooth, erratic, intermittent or lumpy, based on how often and how evenly a product sells |
| Feature | An input column the model learns from, such as last week's sales |
| Gate | The automatic check that a newly trained model beats the baseline before it is used |
| Horizon | How many days ahead a forecast goes (1 or 7 here) |
| Intermittent demand | Products that sell on some days and not others |
| Leakage | Information from the future getting into training, which makes a model look better than it is |
| LightGBM | The gradient-boosted decision tree library used for the model |
| MAE, RMSE, WAPE | Error measures; see section 2.5 |
| M5 | A public Walmart sales dataset and forecasting competition (2020) |
| Model card | A plain-language summary of a model: what it was trained on, how it was tested, its limits |
| Recursive forecast | Predicting several days by feeding each day's prediction back in as input |
| Replay | A forecast made from a past date, so it can be compared with what really sold (research version) |
| Tweedie | A training objective suited to counts with many zeros and occasional spikes |
| Weights | The learned numbers inside a trained model (for LightGBM, its decision trees) |

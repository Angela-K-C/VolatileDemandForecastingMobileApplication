package com.example.volatiledemandforecasting.data

import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.data.store.ActiveModelDto
import com.example.volatiledemandforecasting.data.store.CapabilitiesDto
import com.example.volatiledemandforecasting.data.catalog.HistoryDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The example bodies below are copied from api.md and the model card, so the DTOs are checked against the real contract. */
class ForecastContractTest {
    private val json: Json = Network.json

    @Test
    fun `a completed replay from api md decodes with its evidence`() {
        val forecast = json.decodeFromString<ForecastDto>(
            """
            {"forecastId":"f1","status":"COMPLETED","createdAt":"2026-01-01T10:00:00Z","generatedAt":"2026-01-01T10:00:01Z",
             "storeId":"s1","isReference":true,
             "product":{"id":"p1","sku":"FOODS_3_090","name":"FOODS_3_090","unitOfMeasure":"units",
                "demandClass":{"class":"SMOOTH","label":"Steady seller","adi":1.0,"cv2":0.2,"zeroShare":0.0,"observedDays":365,"saleDays":365}},
             "asOfDate":"2016-05-08","isReplay":true,"method":"COMPARE","error":null,
             "horizon":{"startDate":"2016-05-09","endDate":"2016-05-15","days":7,"frequency":"DAILY","validated":true},
             "model":{"id":"m5-ca1-foods","version":"1.1.0-bt","featureSchema":"m5-v1","trainingEndDate":"2016-04-24"},
             "baselines":[{"id":"naive_last_value","label":"Same as last day"}],
             "aiUnavailableReason":null,"priceAssumption":"KNOWN","missingDaysInLookback":0,"warnings":[],
             "points":[{"date":"2016-05-09","ai":57.1206,"baseline":77.0,"seasonalNaive":59.0,"actual":72.0}],
             "history":[{"date":"2016-04-11","actual":48.0}],"predictionIntervals":null,
             "runMetrics":{"window":"2016-05-09/2016-05-15","n":7,"ai":{"mae":18.23},"baseline":{"mae":22.57}},
             "productBacktest":{"productId":"FOODS_3_090","n":28,"window":"2016-04-25/2016-05-22","demandClass":"SMOOTH",
                "ai":{"mae":11.5,"rmse":14.83,"wape":0.195},"naive_last_value":{"mae":13.0,"rmse":16.0,"wape":0.22},
                "seasonal_naive_7":{"mae":14.0,"rmse":17.0,"wape":0.24}},
             "historyVersion":1,"mlengineVersion":"0.1.0","mlRequestId":"fc_1"}
            """.trimIndent()
        )
        assertTrue(forecast.isCompleted)
        assertTrue(forecast.isReplay)
        assertEquals(57.1206, forecast.points.single().ai!!, 1e-9)
        assertEquals(77.0, forecast.points.single().baseline!!, 1e-9)
        assertEquals(72.0, forecast.points.single().actual!!, 1e-9)
        assertEquals("Steady seller", forecast.product.demandClass?.label)
        assertEquals(18.23, forecast.runMetrics?.ai?.mae!!, 1e-9)
        assertEquals(13.0, forecast.productBacktest?.naiveLastValue?.mae!!, 1e-9)
        assertEquals("2016-04-25" to "2016-05-22", forecast.productBacktest?.window.windowDates())
        assertEquals("2016-05-09" to "2016-05-15", "2016-05-09/2016-05-15".split('/').let { it[0] to it[1] })
    }

    @Test
    fun `a job that has not completed carries only the header and the error`() {
        val forecast = json.decodeFromString<ForecastDto>(
            """{"forecastId":"f2","status":"FAILED","createdAt":"x","generatedAt":null,"storeId":"s1","isReference":false,
                "product":{"id":"p1","sku":"A","name":"A","unitOfMeasure":"units","demandClass":null},
                "asOfDate":"2026-01-01","isReplay":false,"method":"AI",
                "error":{"code":"PRODUCT_NOT_IN_MODEL","message":"This product isn't in the model."}}"""
        )
        assertFalse(forecast.isCompleted)
        assertTrue(forecast.points.isEmpty())
        assertEquals("PRODUCT_NOT_IN_MODEL", forecast.error?.code)
        assertNull(forecast.horizon)
    }

    @Test
    fun `a true forecast has no actuals and a baseline only forecast has no AI`() {
        val forecast = json.decodeFromString<ForecastDto>(
            """{"forecastId":"f3","status":"COMPLETED","storeId":"s","product":{"id":"p","sku":"A","name":"A"},
                "asOfDate":"2016-05-22","isReplay":false,"method":"BASELINE","priceAssumption":"LAST_KNOWN",
                "horizon":{"days":2,"validated":false},
                "points":[{"date":"2016-05-23","ai":null,"baseline":3.0,"seasonalNaive":null,"actual":null}]}"""
        )
        assertNull(forecast.points.single().ai)
        assertNull(forecast.points.single().actual)
        assertEquals(false, forecast.horizon?.validated)
        assertEquals("LAST_KNOWN", forecast.priceAssumption)
    }

    @Test
    fun `capabilities decode the editable as-of range`() {
        val caps = json.decodeFromString<CapabilitiesDto>(
            """{"storeId":"s","isReference":true,"aiAvailable":true,"aiUnavailableReason":null,
                "methods":["AI","BASELINE","COMPARE"],"horizons":[1,7],"frequency":"DAILY",
                "asOf":{"default":"2016-05-22","editable":true,"replayMin":"2016-02-01","replayMax":"2016-05-21","researchMin":"2015-01-01","calendarEnd":"2016-06-19"},
                "minHistoryDays":21,"maxMissingDaysInLookback":3,"predictionIntervals":false,
                "baselines":[{"id":"naive_last_value","label":"Same as last day"}],
                "activeModel":{"id":"m5-ca1-foods","version":"1.1.0"},"training":null}"""
        )
        assertEquals(listOf(1, 7), caps.horizons)
        assertTrue(caps.asOf.editable)
        assertEquals("2016-05-22", caps.asOf.default)
        assertNotNull(caps.activeModel)
    }

    @Test
    fun `the model card decodes with its backtest and accuracy by day`() {
        val model = json.decodeFromString<ActiveModelDto>(
            """{"model":{"id":"m5-ca1-foods","version":"1.1.0","trainingEndDate":"2016-05-22","featureSchema":"m5-v1"},
                "capabilities":{"horizons":[1,7]},
                "card":{"title":"Demand model: LightGBM (Tweedie)","model":"m5-ca1-foods@1.1.0",
                  "trainedOn":{"data":"M5","products":1437,"startDate":"2011-02-19","endDate":"2016-05-22"},
                  "predicts":"Expected units sold per product per day.","horizonDays":[1,7],
                  "inputsPlainLanguage":["Sales over the last 1, 7, 14 and 21 days"],
                  "backtest":{"window":{"startDay":1914,"endDay":1941,"startDate":"2016-04-25","endDate":"2016-05-22"},
                    "series":"all CA_1 FOODS items","oneStepAhead":true,
                    "ai":{"n":40236,"mae":1.3766,"rmse":2.3773,"wape":0.6098},
                    "naive_last_value":{"n":40236,"mae":1.7285,"rmse":3.2186,"wape":0.7657},
                    "seasonal_naive_7":{"n":40236,"mae":1.7481,"rmse":3.1793,"wape":0.7744},
                    "byDemandClass":{"SMOOTH":{"products":221,"ai":{"n":6188,"mae":2.4,"rmse":3.7,"wape":0.4},"naive_last_value":{"n":6188,"mae":3.3,"rmse":5.2,"wape":0.5}}}},
                  "horizonAccuracy":{"description":"d","validated":true,"byDay":[{"day":1,"aiRmse":2.337,"aiMae":1.37,"naiveRmse":3.19,"naiveMae":1.74}]},
                  "limitations":["Validated on grocery (FOODS) products in one store only."],"note":"n"}}"""
        )
        val backtest = model.card.backtest!!
        assertEquals(2.3773, backtest.ai?.rmse!!, 1e-9)
        assertEquals(3.2186, backtest.naiveLastValue?.rmse!!, 1e-9)
        assertEquals("2016-04-25" to "2016-05-22", backtest.window.windowDates())
        assertEquals(1, model.card.horizonAccuracy?.byDay?.size)
        assertEquals(1, model.card.limitations.size)
    }

    @Test
    fun `history keeps a not recorded day as null rather than zero`() {
        val history = json.decodeFromString<HistoryDto>(
            """{"productId":"p","from":"2026-01-01","to":"2026-01-03","points":[
                {"date":"2026-01-01","units":0,"price":1.5},{"date":"2026-01-02","units":null,"price":null},{"date":"2026-01-03","units":4,"price":1.5}]}"""
        )
        assertEquals(0.0, history.points[0].units!!, 0.0)
        assertNull(history.points[1].units)
    }
}

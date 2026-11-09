package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.data.forecast.BatchDto
import com.example.volatiledemandforecasting.data.forecast.BatchItemPageDto
import com.example.volatiledemandforecasting.data.forecast.ForecastPointDto
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownMethodOf
import com.example.volatiledemandforecasting.data.forecast.shownTotal
import com.example.volatiledemandforecasting.data.forecast.usedFor
import com.example.volatiledemandforecasting.data.store.BacktestDto
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import com.example.volatiledemandforecasting.feature.workspace.PickerData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The business version: one number per product, from AI only when it earned it. */
class StoreForecastTest {
    private val json: Json = Network.json

    private fun backtest(body: String) = json.decodeFromString<BacktestDto>(body)

    @Test
    fun `AI is shown only when it beat the simple forecast`() {
        assertEquals(ShownMethod.Ai, shownMethodOf(backtest("""{"ai":{"rmse":2.4,"mae":1.7},"naive_last_value":{"rmse":3.2,"mae":2.1}}""")))
        assertEquals(ShownMethod.Simple, shownMethodOf(backtest("""{"ai":{"rmse":3.5},"naive_last_value":{"rmse":3.2}}""")))
        assertEquals(ShownMethod.Simple, shownMethodOf(backtest("""{"ai":{"rmse":3.2},"naive_last_value":{"rmse":3.2}}""")))
        // MAE decides when RMSE isn't on both sides.
        assertEquals(ShownMethod.Ai, shownMethodOf(backtest("""{"ai":{"mae":1.0},"naive_last_value":{"mae":2.0,"rmse":3.0}}""")))
    }

    @Test
    fun `no model or no numbers means the simple forecast`() {
        assertEquals(ShownMethod.Simple, shownMethodOf(null))
        assertEquals(ShownMethod.Simple, shownMethodOf(backtest("""{"ai":{"mae":1.0}}""")))
    }

    @Test
    fun `a product without AI values falls back to the simple forecast`() {
        val withAi = listOf(ForecastPointDto("2026-11-09", ai = 3.0, baseline = 5.0), ForecastPointDto("2026-11-10", ai = 4.0, baseline = 5.0))
        val withoutAi = listOf(ForecastPointDto("2026-11-09", ai = null, baseline = 5.0))
        assertEquals(ShownMethod.Ai, ShownMethod.Ai.usedFor(withAi))
        assertEquals(ShownMethod.Simple, ShownMethod.Ai.usedFor(withoutAi))
        assertEquals(ShownMethod.Simple, ShownMethod.Simple.usedFor(withAi))
        assertEquals(7.0, withAi.shownTotal(ShownMethod.Ai)!!, 1e-9)
        assertEquals(10.0, withAi.shownTotal(ShownMethod.Simple)!!, 1e-9)
    }

    @Test
    fun `a missing day never counts as zero in the total`() {
        val gap = listOf(ForecastPointDto("2026-11-09", baseline = 5.0), ForecastPointDto("2026-11-10", baseline = null))
        assertNull(gap.shownTotal(ShownMethod.Simple))
        assertNull(emptyList<ForecastPointDto>().shownTotal(ShownMethod.Simple))
    }

    @Test
    fun `a batch and its items from api md decode`() {
        val batch = json.decodeFromString<BatchDto>(
            """{"batchId":"b1","storeId":"s1","status":"RUNNING","method":"COMPARE","asOfDate":"2026-11-08",
                "horizonDays":7,"total":100,"completed":40,"failed":0,"pending":60,
                "createdAt":"2026-11-09T08:00:00Z","finishedAt":null,"outdated":false}"""
        )
        assertTrue(batch.isRunning)
        assertEquals(60, batch.pending)
        val page = json.decodeFromString<BatchItemPageDto>(
            """{"page":1,"pageSize":50,"total":1,"items":[{"forecastId":"f1","productId":"p1","sku":"SKU-1",
                "productName":"Maize flour 2kg","unitOfMeasure":"units","category":"Flour","demandLabel":"Steady seller",
                "status":"COMPLETED","error":null,"aiUnavailableReason":null,
                "points":[{"date":"2026-11-09","ai":12.4,"baseline":11.0,"seasonalNaive":13.0,"actual":null}]}]}"""
        )
        assertEquals(11.0, page.items.single().points.shownTotal(ShownMethod.Simple)!!, 1e-9)
    }

    @Test
    fun `the sample shop is only flagged while it is the current store`() {
        val sample = json.decodeFromString<WorkspaceDto>("""{"id":"w0","name":"Sample","role":"VIEWER","isReference":true}""") to
            json.decodeFromString<StoreDto>("""{"id":"s0","workspaceId":"w0","code":"CA_1","name":"CA_1","country":"US","timezone":"America/Los_Angeles","currency":"USD"}""")
        assertTrue(PickerData(sample, own = emptyList(), currentStoreId = "s0").viewingSample)
        assertFalse(PickerData(sample, own = emptyList(), currentStoreId = "s1").viewingSample)
        assertFalse(PickerData(null, own = emptyList(), currentStoreId = "s0").viewingSample)
    }
}

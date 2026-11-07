package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.ForecastPointDto
import com.example.volatiledemandforecasting.data.forecast.ForecastProductDto
import com.example.volatiledemandforecasting.data.forecast.RecentSalesDto
import com.example.volatiledemandforecasting.data.store.CapabilitiesDto
import com.example.volatiledemandforecasting.feature.dashboard.AiStatus
import com.example.volatiledemandforecasting.feature.dashboard.aiStatusOf
import com.example.volatiledemandforecasting.feature.forecasts.config.ConfigData
import com.example.volatiledemandforecasting.feature.forecasts.progress.PollingSchedule
import com.example.volatiledemandforecasting.feature.forecasts.result.Verdict
import com.example.volatiledemandforecasting.feature.forecasts.result.buildChartModel
import com.example.volatiledemandforecasting.feature.forecasts.result.totalsOf
import com.example.volatiledemandforecasting.feature.forecasts.result.verdictOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

class ForecastPresentationTest {

    private fun forecast(points: List<ForecastPointDto>, history: List<RecentSalesDto> = emptyList(), asOf: String = "2016-05-08") =
        ForecastDto(
            forecastId = "f", status = "COMPLETED", storeId = "s", product = ForecastProductDto("p", "A", "A"),
            asOfDate = asOf, method = "COMPARE", points = points, history = history,
        )

    @Test
    fun `chart lays history then forecast on one axis and leaves unrecorded days as gaps`() {
        val model = buildChartModel(
            forecast(
                points = listOf(ForecastPointDto("2016-05-09", ai = 57.1, baseline = 77.0, actual = 72.0)),
                history = listOf(RecentSalesDto("2016-05-06", 40.0), RecentSalesDto("2016-05-08", 50.0)),
            )
        )!!
        assertEquals(4, model.dayCount)
        assertEquals(listOf(40.0, null, 50.0, null), model.history)
        assertEquals(listOf(null, null, null, 57.1), model.ai)
        assertEquals(72.0, model.actual[3]!!, 0.0)
        assertEquals(2, model.markerIndex)
    }

    @Test
    fun `a forecast without history or points has no chart`() {
        assertNull(buildChartModel(forecast(emptyList())))
    }

    @Test
    fun `totals are summed from raw values and withheld when a day is missing`() {
        val full = totalsOf(listOf(ForecastPointDto("a", ai = 1.26, baseline = 2.0), ForecastPointDto("b", ai = 1.26, baseline = 2.0)))
        assertEquals(2.52, full.ai!!, 1e-9)
        assertNull(full.actual)
        val gap = totalsOf(listOf(ForecastPointDto("a", ai = 1.0), ForecastPointDto("b", ai = null)))
        assertNull(gap.ai)
    }

    @Test
    fun `the verdict says plainly when the baseline did better`() {
        assertEquals(Verdict.AiBetter, verdictOf(2.377, 3.219))
        assertEquals(Verdict.BaselineBetter, verdictOf(3.5, 3.2))
        assertEquals(Verdict.Tie, verdictOf(2.0, 2.0))
        assertEquals(Verdict.Unknown, verdictOf(null, 3.0))
    }

    @Test
    fun `numbers show one decimal by default and whole units on request`() {
        assertEquals("57.1", Formatting.units(57.1206, TableDisplay.Decimals).replace(',', '.'))
        assertEquals("57", Formatting.units(57.1206, TableDisplay.WholeUnits))
        assertEquals("–", Formatting.units(null))
    }

    @Test
    fun `polling is every second for ten seconds, then every three, and stops after two minutes`() {
        assertEquals(1.seconds, PollingSchedule.delayAfter(0.seconds))
        assertEquals(1.seconds, PollingSchedule.delayAfter(9.seconds))
        assertEquals(3.seconds, PollingSchedule.delayAfter(10.seconds))
        assertFalse(PollingSchedule.isExpired(119.seconds))
        assertTrue(PollingSchedule.isExpired(120.seconds))
    }

    @Test
    fun `AI status follows the capabilities`() {
        fun caps(available: Boolean, reason: String?) = CapabilitiesDto(storeId = "s", aiAvailable = available, aiUnavailableReason = reason)
        assertEquals(AiStatus.Available, aiStatusOf(caps(true, null)))
        assertEquals(AiStatus.Training, aiStatusOf(caps(false, "TRAINING")))
        assertEquals(AiStatus.NotEligible, aiStatusOf(caps(false, "NOT_ELIGIBLE")))
        assertEquals(AiStatus.BaselinesOnly, aiStatusOf(caps(false, "GATE_FAILED")))
    }

    @Test
    fun `only dates the API allows can be chosen, and the default is a true forecast`() {
        val caps = com.example.volatiledemandforecasting.data.store.CapabilitiesDto(
            storeId = "s",
            asOf = com.example.volatiledemandforecasting.data.store.AsOfRangeDto(
                default = "2016-05-22", editable = true, replayMin = "2016-02-01", replayMax = "2016-05-21",
            ),
        )
        val data = ConfigData(
            store = com.example.volatiledemandforecasting.core.datastore.StoreSelection("w", "W", "VIEWER", true, "s", "S"),
            product = com.example.volatiledemandforecasting.data.catalog.ProductDto("p", "A", "A"),
            capabilities = caps,
        )
        assertTrue(data.isSelectable(LocalDate.parse("2016-05-08")))
        assertTrue(data.isReplay(LocalDate.parse("2016-05-08")))
        assertTrue(data.isSelectable(LocalDate.parse("2016-05-22")))
        assertFalse(data.isReplay(LocalDate.parse("2016-05-22")))
        assertFalse(data.isSelectable(LocalDate.parse("2016-05-30")))
        assertFalse(data.isSelectable(LocalDate.parse("2015-12-31")))
    }
}

package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.research.ResearchApi
import com.example.volatiledemandforecasting.data.research.ResearchRepository
import com.example.volatiledemandforecasting.data.research.ResearchSummaryDto
import com.example.volatiledemandforecasting.feature.research.ResearchDates
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.create
import java.io.ByteArrayOutputStream

class ResearchTest {
    private val json = Network.json

    @Test
    fun `the summary decodes with all three methods per block`() {
        val s = json.decodeFromString<ResearchSummaryDto>(
            """{"storeId":"s","runs":2,"overall":{"n":8,"ai":{"mae":1.0,"rmse":2.0,"wape":0.5},
               "naive_last_value":{"mae":1.5,"rmse":2.5,"wape":0.7},"seasonal_naive_7":{"mae":1.2,"rmse":2.2,"wape":0.6}},
               "byDemandClass":{"SMOOTH":{"n":8,"ai":{"mae":1.0}}},"byDayAhead":{"1":{"n":4},"7":{"n":4}},"note":"Lower error."}"""
        )
        assertEquals(8, s.overall.n)
        assertEquals(2.5, s.overall.naiveLastValue?.rmse!!, 0.0)
        assertEquals(2.2, s.overall.seasonalNaive7?.rmse!!, 0.0)
        assertEquals(setOf("1", "7"), s.byDayAhead.keys)
    }

    @Test
    fun `an empty summary has no days`() {
        val s = json.decodeFromString<ResearchSummaryDto>("""{"storeId":"s","runs":0,"overall":{"n":0},"note":"x"}""")
        assertEquals(0, s.overall.n)
    }

    @Test
    fun `date filters are blank or valid`() {
        assertEquals(null to null, ResearchDates.check("  "))
        assertEquals("2016-05-20" to null, ResearchDates.check("2016-05-20"))
        assertEquals(UiText.Res(R.string.research_date_invalid), ResearchDates.check("20/05/2016").second)
        assertNull(ResearchDates.check("2016-13-40").first)
    }

    @Test
    fun `export sends the store and range and streams the body`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "text/csv").body("forecast_id,store\nf1,TX_1\n").build())
        server.start()
        try {
            val repo = ResearchRepository(Network.retrofit(server.url("/").toString(), Network.publicClient()).create<ResearchApi>(), json)
            val out = ByteArrayOutputStream()
            repo.exportCsv("s1", "2016-05-01", null, out)
            val url = server.takeRequest().url
            assertEquals("/v1/research/export", url.encodedPath)
            assertEquals("s1", url.queryParameter("storeId"))
            assertEquals("2016-05-01", url.queryParameter("from"))
            assertNull(url.queryParameter("to"))
            assertEquals("forecast_id,store\nf1,TX_1\n", out.toString())
        } finally {
            server.close()
        }
    }
}

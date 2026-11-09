package com.example.volatiledemandforecasting.data

import com.example.volatiledemandforecasting.data.sample.SampleSalesFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SampleSalesFileTest {

    private val end = LocalDate.of(2026, 12, 4)
    private val lines = SampleSalesFile.csv(end).trimEnd().lines()
    private val rows = lines.drop(1).map { it.split(',') }

    @Test
    fun `the header uses names the backend maps without asking`() {
        assertEquals("date,sku,product_name,category,units_sold,unit_price", lines.first())
        assertTrue(rows.all { it.size == 6 })
    }

    @Test
    fun `it ends on the given day and spans 300 days`() {
        val dates = rows.map { LocalDate.parse(it[0]) }
        assertEquals(end, dates.max())
        assertEquals(end.minusDays(299), dates.min())
    }

    @Test
    fun `it is big enough for the shop's own AI model`() {
        // The ML engine's OWN_DATA_ELIGIBILITY: 10 products with 21+ days, 120 store days, 4,500 product-days.
        val daysPerProduct = rows.groupingBy { it[1] }.eachCount()
        assertTrue(daysPerProduct.values.count { it >= 21 } >= 10)
        assertTrue(rows.size >= 4_500)
        assertEquals(26, daysPerProduct.size)
        assertEquals(12, daysPerProduct["ENR-250"])
    }

    @Test
    fun `units are whole and not negative, and prices are positive`() {
        assertTrue(rows.all { it[4].toInt() >= 0 && it[5].toDouble() > 0 })
    }

    @Test
    fun `a day's numbers don't depend on when the file was made`() {
        val later = SampleSalesFile.csv(end.plusDays(30)).lines().toSet()
        val shared = lines.drop(1).filter { LocalDate.parse(it.substringBefore(',')) >= end.minusDays(200) }
            .filterNot { it.contains("CRS-150") || it.contains("ENR-250") }
        assertTrue(shared.all { it in later })
    }

    @Test
    fun `Easter is computed correctly`() {
        assertEquals(LocalDate.of(2026, 4, 5), SampleSalesFile.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), SampleSalesFile.easterSunday(2027))
    }

    /** Writes a copy when SAMPLE_SALES_OUT is set: SAMPLE_SALES_OUT=path SAMPLE_SALES_END=yyyy-mm-dd. */
    @Test
    fun `write a copy on request`() {
        val out = System.getenv("SAMPLE_SALES_OUT") ?: return
        val until = System.getenv("SAMPLE_SALES_END")?.let(LocalDate::parse) ?: LocalDate.now().minusDays(1)
        File(out).writeText(SampleSalesFile.csv(until))
    }
}

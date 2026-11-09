package com.example.volatiledemandforecasting.data.sample

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.util.Locale
import java.util.Random
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A made-up sales history for a small Kenyan mini-market, for trying the app and for demos.
 *
 * It ends on the day it is given (normally yesterday), so the app never calls it out of date, and
 * it is large enough for the shop's own AI model: 300 days and 26 products. Each product's sales
 * follow believable patterns (busier weekends, month-end paydays, public holidays, Christmas, the
 * rainy seasons, promotions, a price rise) plus random variation. Two products are new, so the
 * readiness screen has something to explain.
 *
 * The numbers for a product on a date depend only on that product and date, not on the end date,
 * so files made on different days agree where they overlap (new products aside, since their
 * launch is counted back from the end date).
 */
object SampleSalesFile {

    const val FILE_NAME = "sample-shop-sales.csv"
    const val DAYS = 300
    const val HEADER = "date,sku,product_name,category,units_sold,unit_price"

    internal enum class Kind { Staple, Fresh, Drink, Snack, Household, RainyDay }

    internal class Product(
        val sku: String,
        val name: String,
        val category: String,
        /** Average units on an ordinary weekday. */
        val perDay: Double,
        /** Price in Kenyan shillings at the start of the history. */
        val price: Double,
        val kind: Kind,
        /** Days before the end date that the shop started selling it; null means from the start. */
        val launchedDaysAgo: Int? = null,
    )

    internal val products = listOf(
        Product("MLK-500", "Fresh milk 500ml", "Dairy", 38.0, 65.0, Kind.Fresh),
        Product("YGT-500", "Yoghurt 500ml", "Dairy", 6.0, 140.0, Kind.Fresh),
        Product("EGG-TRY", "Eggs (tray of 30)", "Dairy", 2.5, 450.0, Kind.Fresh),
        Product("BRD-WHT", "White bread 400g", "Bakery", 30.0, 65.0, Kind.Fresh),
        Product("BRD-BRN", "Brown bread 400g", "Bakery", 9.0, 70.0, Kind.Fresh),
        Product("FLR-MZ2", "Maize flour 2kg", "Staples", 14.0, 180.0, Kind.Staple),
        Product("FLR-WH2", "Wheat flour 2kg", "Staples", 4.0, 200.0, Kind.Staple),
        Product("SGR-1KG", "Sugar 1kg", "Staples", 11.0, 160.0, Kind.Staple),
        Product("RCE-1KG", "Rice 1kg", "Staples", 6.0, 190.0, Kind.Staple),
        Product("OIL-1L", "Cooking oil 1L", "Staples", 5.0, 330.0, Kind.Staple),
        Product("TEA-250", "Tea leaves 250g", "Staples", 4.0, 150.0, Kind.Staple),
        Product("SLT-1KG", "Salt 1kg", "Staples", 1.2, 45.0, Kind.Staple),
        Product("TOM-KG", "Tomatoes (kg)", "Produce", 9.0, 120.0, Kind.Fresh),
        Product("ONI-KG", "Onions (kg)", "Produce", 7.0, 110.0, Kind.Fresh),
        Product("SKW-BN", "Sukuma wiki (bunch)", "Produce", 16.0, 20.0, Kind.Fresh),
        Product("SDA-500", "Soda 500ml", "Drinks", 22.0, 70.0, Kind.Drink),
        Product("WTR-1L", "Drinking water 1L", "Drinks", 10.0, 60.0, Kind.Drink),
        Product("JCE-1L", "Juice 1L", "Drinks", 3.0, 230.0, Kind.Drink),
        Product("SOP-BAR", "Bar soap 800g", "Household", 3.0, 170.0, Kind.Household),
        Product("TPR-4PK", "Toilet paper (4 rolls)", "Household", 3.5, 160.0, Kind.Household),
        Product("DTG-1KG", "Washing powder 1kg", "Household", 2.2, 300.0, Kind.Household),
        Product("MTC-10", "Matches (10 boxes)", "Household", 0.8, 50.0, Kind.Household),
        Product("CHR-TIN", "Charcoal (tin)", "Household", 4.0, 100.0, Kind.RainyDay),
        Product("UMB-01", "Umbrella", "Household", 0.25, 450.0, Kind.RainyDay),
        Product("CRS-150", "Crisps 150g", "Snacks", 7.0, 120.0, Kind.Snack, launchedDaysAgo = 45),
        Product("ENR-250", "Energy drink 250ml", "Drinks", 6.0, 100.0, Kind.Drink, launchedDaysAgo = 12),
    )

    /** The whole file as CSV text, one row per product per day, ending on [end]. */
    fun csv(end: LocalDate): String = buildString {
        append(HEADER).append('\n')
        rows(end).forEach { append(it).append('\n') }
    }

    internal fun rows(end: LocalDate): Sequence<String> = sequence {
        val start = end.minusDays(DAYS - 1L)
        val holidays = kenyanHolidays(start.year..end.year)
        for (offset in 0 until DAYS) {
            val date = start.plusDays(offset.toLong())
            products.forEachIndexed { index, product ->
                val firstDay = product.launchedDaysAgo?.let { end.minusDays(it - 1L) } ?: start
                if (date.isBefore(firstDay)) return@forEachIndexed
                val (units, price) = day(product, index, date, holidays)
                yield("$date,${product.sku},${product.name},${product.category},$units,${String.format(Locale.US, "%.2f", price)}")
            }
        }
    }

    private fun day(product: Product, index: Int, date: LocalDate, holidays: Set<LocalDate>): Pair<Int, Double> {
        val random = Random(mix(date.toEpochDay() * 1_000_003L + index * 7_919L))
        var expected = product.perDay
        var price = product.price

        expected *= weekday(date.dayOfWeek, product.kind)
        // A growing shop: about 10% busier each year (measured from a fixed date, not from the file's start).
        expected *= 1.0 + 0.10 * (date.toEpochDay() - GROWTH_FROM.toEpochDay()) / 365.0

        // Month-end payday.
        val dom = date.dayOfMonth
        if (dom >= 28 || dom <= 3) expected *= if (product.kind == Kind.Staple) 1.30 else 1.10
        // January is tight after Christmas and school fees.
        if (date.monthValue == 1 && dom in 2..20) expected *= 0.85

        val md = MonthDay.from(date)
        if (date in holidays) {
            expected *= when (product.kind) {
                Kind.Drink, Kind.Snack -> 1.5
                Kind.Fresh -> 1.15
                else -> 0.9
            }
        }
        if (md >= MonthDay.of(12, 18) && md <= MonthDay.of(12, 24)) {
            expected *= when {
                product.sku == "FLR-WH2" -> 2.2
                product.kind == Kind.Drink || product.kind == Kind.Snack -> 1.6
                product.kind == Kind.Staple -> 1.4
                else -> 1.15
            }
        }
        // Long rains (mid-March to May) and short rains (mid-October to early December).
        val rainy = (md >= MonthDay.of(3, 15) && md <= MonthDay.of(5, 31)) || (md >= MonthDay.of(10, 15) && md <= MonthDay.of(12, 10))
        if (rainy) {
            expected *= when {
                product.sku == "UMB-01" -> 6.0
                product.kind == Kind.RainyDay -> 1.4
                product.kind == Kind.Drink -> 0.85
                else -> 1.0
            }
        }
        // Hot months: more drinks.
        if (product.kind == Kind.Drink && date.monthValue in 1..3) expected *= 1.25

        // Staples go up 6% every 1 July.
        if (product.kind == Kind.Staple) {
            val rises = (date.year - 2025) - (if (date < LocalDate.of(date.year, 7, 1)) 1 else 0)
            repeat(rises.coerceAtLeast(0)) { price *= 1.06 }
        }
        // A week-long promotion every eight weeks on a few products.
        if (product.sku in PROMOTED && (date.toEpochDay() + index * 11) % 56 < 7) {
            price *= 0.85
            expected *= 1.4
        }

        // Day-to-day variation: some days are just busier (gamma), then whole units (Poisson).
        val units = poisson(random, expected * gamma(random, SHAPE) / SHAPE)
        return units to price
    }

    private val PROMOTED = setOf("SDA-500", "OIL-1L", "JCE-1L", "DTG-1KG", "BRD-BRN")
    private const val SHAPE = 8.0
    private val GROWTH_FROM: LocalDate = LocalDate.of(2026, 1, 1)

    private fun weekday(day: DayOfWeek, kind: Kind): Double {
        val shop = when (day) {
            DayOfWeek.MONDAY -> 0.95
            DayOfWeek.TUESDAY -> 0.90
            DayOfWeek.WEDNESDAY -> 0.92
            DayOfWeek.THURSDAY -> 0.95
            DayOfWeek.FRIDAY -> 1.08
            DayOfWeek.SATURDAY -> 1.25
            DayOfWeek.SUNDAY -> 1.02
        }
        val weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
        return if (weekend && (kind == Kind.Drink || kind == Kind.Snack)) shop * 1.2 else shop
    }

    /** Kenya's fixed public holidays plus Good Friday and Easter Monday. */
    internal fun kenyanHolidays(years: IntRange): Set<LocalDate> = years.flatMap { year ->
        val easter = easterSunday(year)
        listOf(
            LocalDate.of(year, 1, 1), easter.minusDays(2), easter.plusDays(1), LocalDate.of(year, 5, 1),
            LocalDate.of(year, 6, 1), LocalDate.of(year, 10, 10), LocalDate.of(year, 10, 20),
            LocalDate.of(year, 12, 12), LocalDate.of(year, 12, 25), LocalDate.of(year, 12, 26),
        )
    }.toSet()

    /** Anonymous Gregorian algorithm. */
    internal fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Spreads nearby seeds apart (SplitMix64 finaliser); java.util.Random alone correlates them. */
    private fun mix(seed: Long): Long {
        var z = seed + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Marsaglia and Tsang, for shape >= 1. Mean equals [shape]. */
    private fun gamma(random: Random, shape: Double): Double {
        val d = shape - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            val x = random.nextGaussian()
            val v = (1.0 + c * x).let { it * it * it }
            if (v <= 0) continue
            val u = random.nextDouble()
            if (ln(u) < 0.5 * x * x + d - d * v + d * ln(v)) return d * v
        }
    }

    private fun poisson(random: Random, mean: Double): Int {
        if (mean <= 0) return 0
        if (mean > 30) return maxOf(0, (mean + sqrt(mean) * random.nextGaussian()).roundToInt())
        val limit = exp(-mean)
        var k = 0
        var p = random.nextDouble()
        while (p > limit) {
            k++
            p *= random.nextDouble()
        }
        return k
    }
}

package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.imports.DetectedDto
import com.example.volatiledemandforecasting.data.imports.ImportApi
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportField
import com.example.volatiledemandforecasting.data.imports.ImportRepository
import com.example.volatiledemandforecasting.data.imports.SuggestedColumnDto
import com.example.volatiledemandforecasting.data.training.EligibilityDto
import com.example.volatiledemandforecasting.data.training.TrainingOutcome
import com.example.volatiledemandforecasting.data.training.TrainingRunDto
import com.example.volatiledemandforecasting.feature.data.wizard.MappingProblem
import com.example.volatiledemandforecasting.feature.data.wizard.MappingRules
import com.example.volatiledemandforecasting.feature.workspace.create.StoreFormValidation
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.create

class ImportWizardTest {

    private val json = Network.json
    private val server = MockWebServer()

    @After
    fun tearDown() = server.close()

    private fun detected(ambiguous: Boolean = false) = DetectedDto(
        suggestedMapping = mapOf(
            ImportField.SALES_DATE to SuggestedColumnDto("sales_date", "HIGH"),
            ImportField.UNITS_SOLD to SuggestedColumnDto("units", "CHECK"),
        ),
        dateFormats = listOf("DD/MM/YYYY", "MM/DD/YYYY"),
        dateFormatAmbiguous = ambiguous,
    )

    @Test
    fun `the mapping starts from the suggestions and flags the unsure ones`() {
        val prefill = MappingRules.prefill(detected())
        assertEquals("sales_date", prefill[ImportField.SALES_DATE])
        assertTrue(detected().suggestedMapping.getValue(ImportField.UNITS_SOLD).needsCheck)
        assertFalse(detected().suggestedMapping.getValue(ImportField.SALES_DATE).needsCheck)
    }

    @Test
    fun `the zero question has no default and must be answered`() {
        val mapping = mapOf<String, String?>(ImportField.SALES_DATE to "d", ImportField.UNITS_SOLD to "u", ImportField.PRODUCT_NAME to "n")
        assertEquals(listOf(MappingProblem.ZeroQuestion), MappingRules.problems(mapping, detected(), null, null))
        assertTrue(MappingRules.problems(mapping, detected(), null, false).isEmpty())
        assertTrue(MappingRules.problems(mapping, detected(), null, true).isEmpty())
    }

    @Test
    fun `required fields, a product identity and an ambiguous date format are all checked`() {
        val problems = MappingRules.problems(emptyMap(), detected(ambiguous = true), null, true)
        assertTrue(MappingProblem.SalesDate in problems)
        assertTrue(MappingProblem.UnitsSold in problems)
        assertTrue(MappingProblem.ProductIdentity in problems)
        assertTrue(MappingProblem.DateFormat in problems)

        val ok = mapOf<String, String?>(ImportField.SALES_DATE to "d", ImportField.UNITS_SOLD to "u", ImportField.PRODUCT_ID to "p")
        assertTrue(MappingRules.problems(ok, detected(ambiguous = true), "DD/MM/YYYY", true).isEmpty())
    }

    @Test
    fun `one column can't feed two fields`() {
        val mapping = mapOf<String, String?>(ImportField.SALES_DATE to "x", ImportField.UNITS_SOLD to "x", ImportField.PRODUCT_ID to "p")
        assertTrue(MappingProblem.DuplicateColumn in MappingRules.problems(mapping, detected(), null, true))
    }

    @Test
    fun `only mapped fields are sent`() {
        val sent = MappingRules.toRequestColumns(mapOf(ImportField.SALES_DATE to "d", ImportField.CATEGORY to null, ImportField.UNIT_PRICE to " "))
        assertEquals(mapOf(ImportField.SALES_DATE to "d"), sent)
    }

    @Test
    fun `codes are made readable`() {
        assertEquals("Invalid date", MappingRules.humanize("INVALID_DATE"))
    }

    @Test
    fun `an upload sends the file field and the idempotency key`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(201).addHeader("Content-Type", "application/json").body(
                """{"id":"i1","storeId":"s1","filename":"sales.csv","sizeBytes":12,"status":"UPLOADED",
                    "columns":["sales_date","units"],"detected":{"delimiter":",","preview":[{"sales_date":"2026-10-01"}],
                    "suggestedMapping":{"salesDate":{"column":"sales_date","confidence":"HIGH"}},
                    "dateFormats":["YYYY-MM-DD"],"suggestedDateFormat":"YYYY-MM-DD","dateFormatAmbiguous":false,"unusedColumns":["revenue"]},
                    "mapping":null,"missingDaysAreZero":null,"summary":null,"result":null}"""
            ).build()
        )
        server.start()
        val repository = ImportRepository(Network.retrofit(server.url("/").toString(), Network.publicClient()).create<ImportApi>(), json)

        val result = repository.upload("s1", "key-123", "sales.csv", "a,b\n1,2\n".toByteArray())

        val request = server.takeRequest()
        assertEquals("/v1/stores/s1/imports", request.url.encodedPath)
        assertEquals("key-123", request.headers["Idempotency-Key"])
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("""name="file"; filename="sales.csv""""))
        assertTrue(body.contains("a,b"))
        assertEquals("UPLOADED", result.status)
        assertEquals(listOf("revenue"), result.detected?.unusedColumns)
    }

    @Test
    fun `a mapping request omits an unneeded date format`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(IMPORT_JSON).build())
        server.start()
        val repository = ImportRepository(Network.retrofit(server.url("/").toString(), Network.publicClient()).create<ImportApi>(), json)

        repository.mapping("i1", com.example.volatiledemandforecasting.data.imports.MappingRequest(mapOf("salesDate" to "d"), null, false))

        val body = server.takeRequest().body!!.utf8()
        assertTrue(body.contains(""""missingDaysAreZero":false"""))
        assertFalse(body.contains("dateFormat"))
    }

    @Test
    fun `a validated import decodes its summary`() {
        val import = json.decodeFromString<ImportDto>(IMPORT_JSON)
        assertEquals("NEEDS_ATTENTION", import.status)
        assertEquals(2, import.summary?.rowsRejected)
        assertEquals(1, import.summary?.errorsByCode?.get("INVALID_DATE"))
        assertEquals("2026-09-08", import.summary?.dateRange?.from)
    }

    @Test
    fun `eligibility and a trained run decode as in api md`() {
        val eligibility = json.decodeFromString<EligibilityDto>(
            """{"workspaceId":"w","eligible":false,"storesInModel":["NBO-1"],
                "checks":{"productsWithEnoughHistory":{"value":30,"required":10,"passed":true},
                          "storeHistoryDays":{"value":60,"required":120,"passed":false}},
                "rules":{},"latestRun":null}"""
        )
        assertFalse(eligibility.eligible)
        assertEquals(60.0, eligibility.checks.getValue("storeHistoryDays").value!!, 0.0)
        assertEquals(120.0, eligibility.checks.getValue("storeHistoryDays").required!!, 0.0)
        assertNull(eligibility.latestRun)

        val run = json.decodeFromString<TrainingRunDto>(
            """{"id":"r1","workspaceId":"w","status":"COMPLETED","outcome":"TRAINED","trigger":"MANUAL",
                "createdAt":"x","finishedAt":"y","historyRows":35095,"model":{"id":"own-w","version":"1.0.0"},"horizons":[1,7],
                "backtest":{"window":{"startDate":"2026-10-10","endDate":"2026-11-06"},"oneStepAhead":true,
                  "ai":{"n":2800,"mae":1.312,"rmse":2.503,"wape":0.573},"naive_last_value":{"n":2800,"mae":1.566,"rmse":3.062,"wape":0.684}},
                "gate":{"passed":true,"rule":"AI RMSE and MAE both below naive_last_value"},
                "horizonAccuracy":{"validated":true,"byDay":[{"day":1,"aiRmse":2.43,"naiveRmse":2.96}]},"error":null,"retryable":false}"""
        )
        assertEquals(TrainingOutcome.TRAINED, run.outcome)
        assertEquals(listOf(1, 7), run.horizons)
        assertEquals(2.503, run.backtest?.ai?.rmse!!, 1e-9)
        assertNotNull(run.gate)
    }

    @Test
    fun `store form checks need no server`() {
        assertNull(StoreFormValidation.country("US"))
        assertEquals(UiText.Res(R.string.create_country_invalid), StoreFormValidation.country("USA"))
        assertNull(StoreFormValidation.currency("usd"))
        assertEquals(UiText.Res(R.string.create_currency_invalid), StoreFormValidation.currency("US"))
        assertNull(StoreFormValidation.timezone("America/Chicago"))
        assertEquals(UiText.Res(R.string.create_timezone_invalid), StoreFormValidation.timezone("Mars/Base"))
        assertEquals(UiText.Res(R.string.create_required), StoreFormValidation.required("  "))
    }

    private companion object {
        const val IMPORT_JSON = """{"id":"i1","storeId":"s1","filename":"sales.csv","sizeBytes":2048,"status":"NEEDS_ATTENTION",
            "columns":["a"],"detected":null,"mapping":{},"missingDaysAreZero":false,
            "summary":{"rowsRead":10,"rowsAccepted":8,"rowsRejected":2,"rowsWithWarnings":1,"rowsSummedAsDuplicates":0,"dailyRecords":8,
              "zeroFilledDays":0,"closedDays":0,"dateRange":{"from":"2026-09-08","to":"2026-11-06"},"products":3,"newProducts":3,
              "stores":["NBO-1"],"overlappingDays":0,"errorsByCode":{"INVALID_DATE":1,"NEGATIVE_UNITS":1},"warningsByCode":{"SPIKE":1},
              "issuesTruncated":false,"unusedColumns":[]},"result":null}"""
    }
}

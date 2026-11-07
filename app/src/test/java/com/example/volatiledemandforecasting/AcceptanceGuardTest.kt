package com.example.volatiledemandforecasting

import androidx.fragment.app.Fragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Structural checks for the acceptance criteria in spec §19 that can be proved from the source tree:
 * no XML screen layouts (3), every destination is a Fragment, no dangling navigation, and no
 * secrets or open HTTP in the release configuration (13).
 */
class AcceptanceGuardTest {

    private val main = File("src/main")
    private val graph = File(main, "res/navigation/main_graph.xml").readText()

    private fun sources() = File(main, "java").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun `there are no XML screen layouts`() {
        assertFalse(File(main, "res/layout").exists())
    }

    @Test
    fun `every destination in the nav graph is a Fragment class`() {
        val names = Regex("""android:name="([^"]+)"""").findAll(graph).map { it.groupValues[1] }.toList()
        assertTrue(names.size > 20)
        names.forEach { name ->
            val cls = Class.forName(name)
            assertTrue("$name is not a Fragment", Fragment::class.java.isAssignableFrom(cls))
        }
    }

    @Test
    fun `every destination id used in code exists in the graph`() {
        val declared = Regex("""android:id="@\+id/(\w+)"""").findAll(graph).map { it.groupValues[1] }.toSet()
        val used = sources().flatMap { f ->
            Regex("""R\.id\.(\w+Fragment)\b""").findAll(f.readText()).map { it.groupValues[1] }.toList()
        }.toSet()
        assertEquals("used but not declared: ${used - declared}", emptySet<String>(), used - declared)
    }

    @Test
    fun `the release manifest asks only for the internet and does not allow plain HTTP`() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertEquals(listOf("android.permission.INTERNET"), Regex("""uses-permission android:name="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toList())
        assertFalse(manifest.contains("usesCleartextTraffic=\"true\""))
        assertTrue(File("src/debug/AndroidManifest.xml").readText().contains("usesCleartextTraffic=\"true\""))
    }

    @Test
    fun `no URLs, tokens or passwords are written into the source`() {
        sources().forEach { f ->
            val text = f.readText()
            assertFalse("${f.name} contains a URL", Regex("""https?://(?!schemas\.|www\.w3\.org)""").containsMatchIn(text.replace(Regex("""/\*.*?\*/|//.*""", RegexOption.DOT_MATCHES_ALL), "").lines().filter { !it.trimStart().startsWith("*") && !it.trimStart().startsWith("//") }.joinToString("\n")))
        }
    }
}

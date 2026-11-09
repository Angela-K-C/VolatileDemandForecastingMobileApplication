package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.feature.dashboard.SetupProgress
import com.example.volatiledemandforecasting.feature.dashboard.SetupStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GettingStartedTest {

    @Test
    fun `a new shop is asked to upload sales first`() {
        val progress = SetupProgress(hasSales = false, hasForecast = false, aiReady = false)
        assertEquals(SetupStep.UploadSales, progress.next)
        assertEquals(1, progress.doneCount)
        assertTrue(progress.showChecklist)
    }

    @Test
    fun `after uploading, the next step is the first forecast`() {
        val progress = SetupProgress(hasSales = true, hasForecast = false, aiReady = false)
        assertEquals(SetupStep.FirstForecast, progress.next)
        assertTrue(progress.showChecklist)
    }

    @Test
    fun `waiting for AI alone doesn't keep the checklist on screen`() {
        val progress = SetupProgress(hasSales = true, hasForecast = true, aiReady = false)
        assertEquals(SetupStep.AiUnlocked, progress.next)
        assertFalse(progress.showChecklist)
    }

    @Test
    fun `everything done means no next step`() {
        val progress = SetupProgress(hasSales = true, hasForecast = true, aiReady = true)
        assertNull(progress.next)
        assertEquals(SetupStep.entries.size, progress.doneCount)
    }
}

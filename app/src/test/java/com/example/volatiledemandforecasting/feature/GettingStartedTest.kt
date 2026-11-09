package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.feature.dashboard.AiStage
import com.example.volatiledemandforecasting.feature.dashboard.AiStatus
import com.example.volatiledemandforecasting.feature.dashboard.SetupProgress
import com.example.volatiledemandforecasting.feature.dashboard.SetupStep
import com.example.volatiledemandforecasting.feature.dashboard.aiStageOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GettingStartedTest {

    @Test
    fun `a new shop is asked to upload sales first`() {
        val progress = SetupProgress(hasSales = false, hasForecast = false, ai = AiStage.Waiting)
        assertEquals(SetupStep.UploadSales, progress.next)
        assertEquals(1, progress.doneCount)
        assertTrue(progress.showChecklist)
    }

    @Test
    fun `after uploading, the next step is the first forecast`() {
        val progress = SetupProgress(hasSales = true, hasForecast = false, ai = AiStage.Waiting)
        assertEquals(SetupStep.FirstForecast, progress.next)
        assertTrue(progress.showChecklist)
    }

    @Test
    fun `waiting for enough sales alone doesn't keep the checklist on screen`() {
        val progress = SetupProgress(hasSales = true, hasForecast = true, ai = AiStage.Waiting)
        assertEquals(SetupStep.AiUnlocked, progress.next)
        assertFalse(progress.showChecklist)
    }

    @Test
    fun `the checklist comes back when the AI model can be trained, and while it trains`() {
        assertTrue(SetupProgress(hasSales = true, hasForecast = true, ai = AiStage.CanTrain).showChecklist)
        assertTrue(SetupProgress(hasSales = true, hasForecast = true, ai = AiStage.Training).showChecklist)
    }

    @Test
    fun `everything done means no next step`() {
        val progress = SetupProgress(hasSales = true, hasForecast = true, ai = AiStage.Ready)
        assertNull(progress.next)
        assertEquals(SetupStep.entries.size, progress.doneCount)
        assertFalse(progress.showChecklist)
    }

    @Test
    fun `AI stage follows the store's status and eligibility`() {
        // The backend never trains the first model by itself, so "eligible" means a person has to start it.
        assertEquals(AiStage.CanTrain, aiStageOf(AiStatus.NotEligible, eligible = true))
        assertEquals(AiStage.Waiting, aiStageOf(AiStatus.NotEligible, eligible = false))
        assertEquals(AiStage.Training, aiStageOf(AiStatus.Training, eligible = true))
        assertEquals(AiStage.DidNotBeat, aiStageOf(AiStatus.BaselinesOnly, eligible = true))
        assertEquals(AiStage.Ready, aiStageOf(AiStatus.Available, eligible = false))
    }
}

package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.feature.settings.ChangePasswordRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChangePasswordTest {
    @Test
    fun `new password must be typed twice the same`() {
        assertNull(ChangePasswordRules.confirm("abc", "abc"))
        assertEquals(UiText.Res(R.string.cp_mismatch), ChangePasswordRules.confirm("abc", "abd"))
    }
}

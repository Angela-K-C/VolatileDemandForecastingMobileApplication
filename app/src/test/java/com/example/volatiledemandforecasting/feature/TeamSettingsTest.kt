package com.example.volatiledemandforecasting.feature

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.team.AssignableRoles
import com.example.volatiledemandforecasting.data.team.InvitationDto
import com.example.volatiledemandforecasting.data.team.MemberDto
import com.example.volatiledemandforecasting.feature.settings.ChangePasswordRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TeamSettingsTest {
    private val json = Network.json

    @Test
    fun `members and invitations decode as in api md`() {
        val member = json.decodeFromString<MemberDto>("""{"userId":"u1","email":"a@b.co","displayName":"Ada","role":"OWNER","joinedAt":"2026-10-01T08:00:00Z"}""")
        assertEquals("OWNER", member.role)
        val invitation = json.decodeFromString<InvitationDto>(
            """{"id":"i1","workspaceId":"w","workspaceName":"Shop","email":"c@d.co","role":"VIEWER","status":"PENDING","invitedBy":null,"createdAt":"x","expiresAt":"y"}"""
        )
        assertEquals("PENDING", invitation.status)
        assertNull(invitation.invitedBy)
    }

    @Test
    fun `the demo's read-only role can't be assigned`() {
        assertEquals(listOf("OWNER", "MANAGER", "VIEWER"), AssignableRoles.all)
        assertFalse("REFERENCE" in AssignableRoles.all)
    }

    @Test
    fun `new password must be typed twice the same`() {
        assertNull(ChangePasswordRules.confirm("abc", "abc"))
        assertEquals(UiText.Res(R.string.cp_mismatch), ChangePasswordRules.confirm("abc", "abd"))
    }
}

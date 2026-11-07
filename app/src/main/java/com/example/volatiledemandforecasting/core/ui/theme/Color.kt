package com.example.volatiledemandforecasting.core.ui.theme

import androidx.compose.ui.graphics.Color

// Brand tokens from spec §7.2. Change the palette here, nowhere else.
val Navy = Color(0xFF17324D)
val Teal = Color(0xFF168C86)

// Light
val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF17212B)
val LightOnSurfaceVariant = Color(0xFF5B6B7E)
val LightSurfaceVariant = Color(0xFFE8EDF2)
val LightOutline = Color(0xFF8795A8)
val LightOutlineVariant = Color(0xFFD5DCE4)
val LightPrimaryContainer = Color(0xFFD6E3F0)
val LightOnPrimaryContainer = Color(0xFF0B1D2E)
val LightSecondaryContainer = Color(0xFFCFEDEA)
val LightOnSecondaryContainer = Color(0xFF06403D)
val LightError = Color(0xFFC62828)
val LightErrorContainer = Color(0xFFFDE7E7)
val LightOnErrorContainer = Color(0xFF5C0F0F)

// Dark
val DarkBackground = Color(0xFF0F1720)
val DarkSurface = Color(0xFF16202B)
val DarkOnSurface = Color(0xFFE6EBF0)
val DarkOnSurfaceVariant = Color(0xFFA8B4C3)
val DarkSurfaceVariant = Color(0xFF253240)
val DarkOutline = Color(0xFF7A889A)
val DarkOutlineVariant = Color(0xFF33424F)
val DarkPrimary = Color(0xFF9DBBDA)
val DarkPrimaryContainer = Color(0xFF244766)
val DarkOnPrimaryContainer = Color(0xFFD6E3F0)
val DarkSecondary = Color(0xFF4CC3BC)
val DarkSecondaryContainer = Color(0xFF0F5C58)
val DarkOnSecondaryContainer = Color(0xFFCFEDEA)
val DarkError = Color(0xFFFF8A80)
val DarkErrorContainer = Color(0xFF7A1F1F)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

/** Colours Material 3 has no slot for: status colours and the "Demo data" marker. */
data class AppColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val demoBadge: Color,
    val onDemoBadge: Color,
)

val LightAppColors = AppColors(
    success = Color(0xFF1B7F3B),
    successContainer = Color(0xFFDFF3E4),
    onSuccessContainer = Color(0xFF0C3D1B),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFF1D0),
    onWarningContainer = Color(0xFF4A3000),
    // Violet-grey on purpose: it must never read as success, warning or error.
    demoBadge = Color(0xFFE9E6F5),
    onDemoBadge = Color(0xFF4B3F7A),
)

val DarkAppColors = AppColors(
    success = Color(0xFF7FD59A),
    successContainer = Color(0xFF14432A),
    onSuccessContainer = Color(0xFFDFF3E4),
    warning = Color(0xFFF2C063),
    warningContainer = Color(0xFF4F3A0E),
    onWarningContainer = Color(0xFFFFF1D0),
    demoBadge = Color(0xFF3A3556),
    onDemoBadge = Color(0xFFD9D2F5),
)

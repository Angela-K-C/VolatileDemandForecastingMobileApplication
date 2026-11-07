package com.example.volatiledemandforecasting.app.navigation

import androidx.annotation.StringRes
import android.os.Bundle
import androidx.navigation.NavController
import com.example.volatiledemandforecasting.R

/** Screens shown while signed out (spec §5.2). They have no bottom bar, workspace chip or Options menu. */
val AUTH_DESTINATIONS = setOf(R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment)

const val ARG_PLACEHOLDER_TITLE = "placeholderTitle"
const val ARG_PLACEHOLDER_STEP = "placeholderStep"

/** Opens the "not built yet" screen for a destination that a later build step will provide. */
fun NavController.navigateToPlaceholder(@StringRes titleRes: Int, step: Int) {
    val args = Bundle().apply {
        putInt(ARG_PLACEHOLDER_TITLE, titleRes)
        putInt(ARG_PLACEHOLDER_STEP, step)
    }
    navigate(R.id.placeholderFragment, args)
}

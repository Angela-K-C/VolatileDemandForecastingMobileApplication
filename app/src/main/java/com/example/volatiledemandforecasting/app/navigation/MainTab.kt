package com.example.volatiledemandforecasting.app.navigation

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.example.volatiledemandforecasting.R

/** The four bottom-navigation destinations (spec §5.1). */
enum class MainTab(@param:IdRes val destinationId: Int, @param:StringRes val labelRes: Int, val icon: ImageVector) {
    Dashboard(R.id.dashboardFragment, R.string.nav_dashboard, Icons.Outlined.Dashboard),
    Products(R.id.productsFragment, R.string.nav_products, Icons.Outlined.Inventory2),
    Data(R.id.dataManagementFragment, R.string.nav_data, Icons.Outlined.Storage),
    Forecasts(R.id.forecastsFragment, R.string.nav_forecasts, Icons.Outlined.Timeline),
}

/**
 * Switches tab without stacking duplicates: reselecting a tab does nothing, and each tab
 * keeps its own back stack (spec §5.2).
 */
fun NavController.navigateToTab(tab: MainTab) {
    navigate(
        tab.destinationId,
        null,
        navOptions {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}

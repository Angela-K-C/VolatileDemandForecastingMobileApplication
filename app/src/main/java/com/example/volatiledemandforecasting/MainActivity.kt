package com.example.volatiledemandforecasting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.navigation.fragment.NavHostFragment

/**
 * Owns only the Fragment host (spec §4.2). Each screen is a Fragment that draws its own
 * shell, so there is no activity layout beyond the container.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(FragmentContainerView(this).apply { id = R.id.nav_host_container })

        if (savedInstanceState == null) {
            val navHost = NavHostFragment.create(R.navigation.main_graph)
            supportFragmentManager.commitNow {
                setReorderingAllowed(true)
                replace(R.id.nav_host_container, navHost)
                setPrimaryNavigationFragment(navHost)
            }
        }
    }
}

package com.example.volatiledemandforecasting.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class VolatileApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as VolatileApp).container

val ComponentActivity.appContainer: AppContainer
    get() = (application as VolatileApp).container

/** A ViewModel scoped to this Fragment, built from the [AppContainer]. */
inline fun <reified VM : ViewModel> Fragment.appViewModels(crossinline create: (AppContainer) -> VM) =
    viewModels<VM> { viewModelFactory { initializer { create(appContainer) } } }

/** A ViewModel shared by every Fragment in the Activity, built from the [AppContainer]. */
inline fun <reified VM : ViewModel> Fragment.appActivityViewModels(crossinline create: (AppContainer) -> VM) =
    activityViewModels<VM> { viewModelFactory { initializer { create(appContainer) } } }

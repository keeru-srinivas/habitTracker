package com.example.cozytrack

import android.app.Application
import com.example.cozytrack.core.di.AppContainer

class CozyTrackApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

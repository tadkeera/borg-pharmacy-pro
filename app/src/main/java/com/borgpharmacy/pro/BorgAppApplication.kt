package com.borgpharmacy.pro

import android.app.Application

class BorgAppApplication : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

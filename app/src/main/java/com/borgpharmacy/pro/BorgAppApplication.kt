package com.borgpharmacy.pro

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BorgAppApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch { container.authRepository.restoreSession() }
    }
    override fun onTerminate() { applicationScope.cancel(); super.onTerminate() }
}

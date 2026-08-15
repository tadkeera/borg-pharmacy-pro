package com.borgpharmacy.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.borgpharmacy.pro.ui.navigation.AppNavigation
import com.borgpharmacy.pro.ui.onboarding.FacilityOnboardingViewModel
import com.borgpharmacy.pro.ui.theme.BorgPharmacyProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as BorgAppApplication).container
        setContent {
            BorgPharmacyProTheme {
                val profile by container.facilityRepository.observe().collectAsStateWithLifecycle(initialValue = null)
                if (profile == null) {
                    val onboarding: FacilityOnboardingViewModel = viewModel(
                        factory = FacilityOnboardingViewModel.Factory(container.facilityRepository)
                    )
                    com.borgpharmacy.pro.ui.onboarding.FacilityOnboardingScreen(onboarding)
                } else AppNavigation(container, profile!!)
            }
        }
    }
}

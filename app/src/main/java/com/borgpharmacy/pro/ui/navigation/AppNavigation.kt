package com.borgpharmacy.pro.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.borgpharmacy.pro.ui.onboarding.*
@Composable fun AppNavigation(onboarding:FacilityOnboardingViewModel){val nav=rememberNavController();NavHost(nav,"onboarding"){composable("onboarding"){FacilityOnboardingScreen(onboarding)}}}

package com.borgpharmacy.pro.ui.dashboard
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class DashboardState(val loading:Boolean=false)
sealed interface DashboardEvent
class DashboardViewModel:ViewModel(){private val _state=MutableStateFlow(DashboardState());val state:StateFlow<DashboardState> = _state}

package com.borgpharmacy.pro.ui.settings
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class SettingsState(val loading:Boolean=false)
sealed interface SettingsEvent
class SettingsViewModel:ViewModel(){private val _state=MutableStateFlow(SettingsState());val state:StateFlow<SettingsState> = _state}

package com.borgpharmacy.pro.ui.weekly
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class WeeklyScheduleState(val loading:Boolean=false)
sealed interface WeeklyScheduleEvent
class WeeklyScheduleViewModel:ViewModel(){private val _state=MutableStateFlow(WeeklyScheduleState());val state:StateFlow<WeeklyScheduleState>=_state}

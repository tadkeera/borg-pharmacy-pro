package com.borgpharmacy.pro.ui.home
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class HomeState(val loading:Boolean=false)
sealed interface HomeEvent
class HomeViewModel:ViewModel(){private val _state=MutableStateFlow(HomeState());val state:StateFlow<HomeState> = _state}

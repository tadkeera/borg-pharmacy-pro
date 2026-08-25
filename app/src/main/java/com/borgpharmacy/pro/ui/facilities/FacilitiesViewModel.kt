package com.borgpharmacy.pro.ui.facilities
import androidx.lifecycle.ViewModel
import com.borgpharmacy.pro.domain.model.FacilityProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class FacilitiesState(val profile:FacilityProfile?=null)
sealed interface FacilitiesEvent
class FacilitiesViewModel(initial:FacilityProfile):ViewModel(){private val _state=MutableStateFlow(FacilitiesState(initial));val state:StateFlow<FacilitiesState> = _state}

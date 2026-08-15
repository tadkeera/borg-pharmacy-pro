package com.borgpharmacy.pro.ui.companies
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class CompaniesState(val loading:Boolean=false)
sealed interface CompaniesEvent
class CompaniesViewModel:ViewModel(){private val _state=MutableStateFlow(CompaniesState());val state:StateFlow<CompaniesState>=_state}

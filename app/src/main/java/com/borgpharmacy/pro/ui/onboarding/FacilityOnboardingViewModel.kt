package com.borgpharmacy.pro.ui.onboarding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.borgpharmacy.pro.core.security.PasswordSecurity
import com.borgpharmacy.pro.domain.model.*
import com.borgpharmacy.pro.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class OnboardingState(val arabic:String="",val english:String="",val username:String="admin",val password:String="",val policy:CyclePolicy=CyclePolicy.ONE,val logo:String?=null,val saved:Boolean=false,val error:String?=null)
sealed interface OnboardingEvent { data class Arabic(val v:String):OnboardingEvent; data class English(val v:String):OnboardingEvent; data class Username(val v:String):OnboardingEvent; data class Password(val v:String):OnboardingEvent; data class Policy(val v:CyclePolicy):OnboardingEvent; data class Logo(val v:String?):OnboardingEvent; data object Save:OnboardingEvent }
class FacilityOnboardingViewModel(private val repo:FacilityRepository):ViewModel(){
 companion object { fun Factory(repo:FacilityRepository)=object:androidx.lifecycle.ViewModelProvider.Factory{ @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>)=FacilityOnboardingViewModel(repo) as T } }
 private val _state=MutableStateFlow(OnboardingState()); val state:StateFlow<OnboardingState>=_state
 fun event(e:OnboardingEvent){when(e){is OnboardingEvent.Arabic->_state.update{it.copy(arabic=e.v)};is OnboardingEvent.English->_state.update{it.copy(english=e.v)};is OnboardingEvent.Username->_state.update{it.copy(username=e.v)};is OnboardingEvent.Password->_state.update{it.copy(password=e.v)};is OnboardingEvent.Policy->_state.update{it.copy(policy=e.v)};is OnboardingEvent.Logo->_state.update{it.copy(logo=e.v)};OnboardingEvent.Save->viewModelScope.launch{val s=_state.value;if(s.arabic.isBlank()||s.english.isBlank()||s.username.isBlank()||s.password.length<8)_state.update{it.copy(error="Complete all fields; password must be 8 characters") }else{repo.save(FacilityProfile(arabicName=s.arabic,englishName=s.english,logoPath=s.logo,policy=s.policy,adminUsername=s.username,adminPasswordHash=PasswordSecurity.hash(s.password.toCharArray())));_state.update{it.copy(saved=true)}}}}}
}

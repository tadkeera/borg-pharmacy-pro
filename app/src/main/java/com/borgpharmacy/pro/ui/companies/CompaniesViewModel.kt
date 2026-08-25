package com.borgpharmacy.pro.ui.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.Shift
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CompaniesState(val query:String="",val companies:List<Company> = emptyList(),val expandedId:String?=null,val adding:Boolean=false,val saving:Boolean=false,val error:String?=null)
sealed interface CompaniesEvent { data class Search(val value:String):CompaniesEvent; data class Toggle(val id:String):CompaniesEvent; data object Add:CompaniesEvent; data object Cancel:CompaniesEvent }
class CompaniesViewModel(private val container:AppContainer,private val tenant:String):ViewModel(){
 private val _state=MutableStateFlow(CompaniesState()); val state:StateFlow<CompaniesState> = _state
 init{viewModelScope.launch{container.borgRepository.companies(tenant).collect{items->_state.update{it.copy(companies=if(it.query.isBlank())items else items.filter{c->c.name.contains(it.query,true)})}}}}
 fun onEvent(event:CompaniesEvent){when(event){is CompaniesEvent.Search->_state.update{it.copy(query=event.value)};is CompaniesEvent.Toggle->_state.update{it.copy(expandedId=if(it.expandedId==event.id)null else event.id)};CompaniesEvent.Add->_state.update{it.copy(adding=true,error=null)};CompaniesEvent.Cancel->_state.update{it.copy(adding=false)}}}
 fun saveCompany(name:String,day:Int,shift:Shift){if(name.isBlank()){_state.update{it.copy(error="اسم الشركة مطلوب")};return};viewModelScope.launch{_state.update{it.copy(saving=true,error=null)};runCatching{container.mutationRepository.saveCompany(tenant,Company(name=name.trim(),baseDay=day,baseShift=shift),"CREATE")}.onFailure{_state.update{it.copy(error=it.message)}}.onSuccess{_state.update{it.copy(adding=false)}};_state.update{it.copy(saving=false)}}}
}

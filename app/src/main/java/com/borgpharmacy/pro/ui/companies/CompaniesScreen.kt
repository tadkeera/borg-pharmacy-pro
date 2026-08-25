package com.borgpharmacy.pro.ui.companies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.FacilityProfile
import com.borgpharmacy.pro.domain.model.Shift

@Composable fun CompaniesScreen(container:AppContainer,profile:FacilityProfile){
 val vm=remember(profile.tenantId){CompaniesViewModel(container,profile.tenantId)};val state by vm.state.collectAsState()
 var newName by remember{mutableStateOf("")};var day by remember{mutableIntStateOf(0)};var shift by remember{mutableStateOf(Shift.MORNING)}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("الشركات",style=MaterialTheme.typography.headlineMedium);Text("إدارة شركات الأدوية ضمن ${profile.arabicName}")};FilledTonalIconButton({vm.onEvent(CompaniesEvent.Add)}){Icon(Icons.Default.AddBusiness,"إضافة شركة")}}
  OutlinedTextField(state.query,{vm.onEvent(CompaniesEvent.Search(it))},Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("البحث باسم الشركة")})
  if(state.companies.isEmpty())ElevatedCard(Modifier.fillMaxWidth()){ListItem(headlineContent={Text("لا توجد شركات مطابقة")},supportingContent={Text("أضف شركة دوائية لبدء تعيين المندوبين.")})}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(state.companies,key={it.id}){company->ElevatedCard(Modifier.fillMaxWidth()){ListItem(headlineContent={Text(company.name,style=MaterialTheme.typography.titleMedium)},supportingContent={Text("اليوم الأساسي: ${company.baseDay} • ${company.baseShift.arabicName}")},trailingContent={Text(if(state.expandedId==company.id)"إخفاء" else "تفاصيل")},modifier=Modifier.padding(4.dp).clickable{vm.onEvent(CompaniesEvent.Toggle(company.id))});if(state.expandedId==company.id)Text("بيانات الشركة محفوظة محلياً ومجهزة للمزامنة.",Modifier.padding(16.dp))}}}
  state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }
 if(state.adding)AlertDialog(onDismissRequest={vm.onEvent(CompaniesEvent.Cancel)},confirmButton={TextButton({vm.saveCompany(newName,day,shift)}){Text(if(state.saving)"جار الحفظ…" else "حفظ")}},dismissButton={TextButton({vm.onEvent(CompaniesEvent.Cancel)}){Text("إلغاء")}},title={Text("إضافة شركة دوائية")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(newName,{newName=it},label={Text("اسم الشركة")});Text("اليوم الأساسي: $day");Row{listOf(0,1,2,3,4).forEach{FilterChip(day==it,{day=it},label={Text("$it")});Spacer(Modifier.width(3.dp))}};Text("الفترة");Row{Shift.entries.forEach{FilterChip(shift==it,{shift=it},label={Text(it.arabicName)})}}}})
}

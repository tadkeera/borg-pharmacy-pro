package com.borgpharmacy.pro.ui.facilities
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import kotlinx.coroutines.launch
import com.borgpharmacy.pro.domain.model.*
@Composable fun FacilitiesScreen(container:AppContainer,profile:FacilityProfile){var edit by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();var ar by remember{mutableStateOf(profile.arabicName)};var en by remember{mutableStateOf(profile.englishName)};val vm=remember(profile.id){FacilitiesViewModel(profile)};val state by vm.state.collectAsState();Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("المنشأة",style=MaterialTheme.typography.headlineMedium);ElevatedCard{Column(Modifier.padding(16.dp)){Text(state.profile?.arabicName.orEmpty(),style=MaterialTheme.typography.titleLarge);Text(state.profile?.englishName.orEmpty());HorizontalDivider(Modifier.padding(vertical=12.dp));Text("سياسة الزيارات: ${state.profile?.policy?.visits ?: 0} زيارات لكل دورة")}};OutlinedButton({edit=true}){Text("تعديل بيانات المنشأة")}};if(edit)AlertDialog(onDismissRequest={edit=false},confirmButton={TextButton({scope.launch{container.extendedMutationRepository.saveFacilityProfile(profile.copy(arabicName=ar,englishName=en));edit=false}}){Text("حفظ")}},dismissButton={TextButton({edit=false}){Text("إلغاء")}},title={Text("تعديل الهوية")},text={Column{OutlinedTextField(ar,{ar=it},label={Text("الاسم بالعربية")});OutlinedTextField(en,{en=it},label={Text("English name")})}})}

package com.borgpharmacy.pro.ui.onboarding
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.domain.model.CyclePolicy
@Composable fun FacilityOnboardingScreen(vm:FacilityOnboardingViewModel){val s by vm.state.collectAsState();var ar by rememberSaveable { mutableStateOf("") };var en by rememberSaveable { mutableStateOf("") };var user by rememberSaveable { mutableStateOf("admin") };var pass by rememberSaveable { mutableStateOf("") };Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("إعداد المنشأة",style=MaterialTheme.typography.headlineMedium);OutlinedTextField(ar,{ar=it;vm.event(OnboardingEvent.Arabic(it))},label={Text("اسم المنشأة بالعربية")});OutlinedTextField(en,{en=it;vm.event(OnboardingEvent.English(it))},label={Text("Facility name")});OutlinedTextField(user,{user=it;vm.event(OnboardingEvent.Username(it))},label={Text("اسم المستخدم")});OutlinedTextField(pass,{pass=it;vm.event(OnboardingEvent.Password(it))},label={Text("كلمة المرور")});Text("عدد الزيارات الشهرية");Row{CyclePolicy.entries.forEach{p->FilterChip(selected=s.policy==p,onClick={vm.event(OnboardingEvent.Policy(p))},label={Text("${p.visits}")});Spacer(Modifier.width(4.dp))}};Button({vm.event(OnboardingEvent.Save)},enabled=!s.saved){Text("حفظ")};s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}

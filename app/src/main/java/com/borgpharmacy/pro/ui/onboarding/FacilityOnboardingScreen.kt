package com.borgpharmacy.pro.ui.onboarding
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.domain.model.CyclePolicy
@Composable fun FacilityOnboardingScreen(vm:FacilityOnboardingViewModel){val s by vm.state.collectAsState();Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("إعداد المنشأة",style=MaterialTheme.typography.headlineMedium);var ar by remember{s.arabic};var en by remember{s.english};OutlinedTextField(ar,{ar=it;vm.event(OnboardingEvent.Arabic(it))},label={Text("اسم المنشأة بالعربية")});OutlinedTextField(en,{en=it;vm.event(OnboardingEvent.English(it))},label={Text("Facility name")});Text("Admin credentials");OutlinedTextField(s.username,{vm.event(OnboardingEvent.Username(it))},label={Text("Username")});OutlinedTextField(s.password,{vm.event(OnboardingEvent.Password(it))},label={Text("Initial password")});Text("Monthly visits");Row{CyclePolicy.entries.forEach{p->FilterChip(selected=s.policy==p,onClick={vm.event(OnboardingEvent.Policy(p))},label={Text("${p.visits}")});Spacer(Modifier.width(4.dp))}};Button({vm.event(OnboardingEvent.Save)},enabled=!s.saved){Text("حفظ")};s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}

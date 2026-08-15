package com.borgpharmacy.pro.ui.companies
import androidx.compose.material3.*
import androidx.compose.runtime.*
@Composable fun CompaniesScreen(vm:CompaniesViewModel){val state by vm.state.collectAsState();Text("Companies")}

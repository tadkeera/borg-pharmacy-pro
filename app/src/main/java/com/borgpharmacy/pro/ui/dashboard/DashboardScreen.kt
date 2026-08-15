package com.borgpharmacy.pro.ui.dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
@Composable fun DashboardScreen(vm:DashboardViewModel){val state by vm.state.collectAsState();Text("Dashboard")}

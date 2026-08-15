package com.borgpharmacy.pro.ui.weekly
import androidx.compose.material3.*
import androidx.compose.runtime.*
@Composable fun WeeklyScheduleScreen(vm:WeeklyScheduleViewModel){val state by vm.state.collectAsState();Text("WeeklySchedule")}

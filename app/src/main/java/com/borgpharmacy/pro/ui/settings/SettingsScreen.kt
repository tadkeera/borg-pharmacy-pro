package com.borgpharmacy.pro.ui.settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
@Composable fun SettingsScreen(vm:SettingsViewModel){val state by vm.state.collectAsState();Text("Settings")}

package com.borgpharmacy.pro.ui.home
import androidx.compose.material3.*
import androidx.compose.runtime.*
@Composable fun HomeScreen(vm:HomeViewModel){val state by vm.state.collectAsState();Text("Home")}

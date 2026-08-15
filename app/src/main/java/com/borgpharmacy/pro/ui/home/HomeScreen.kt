package com.borgpharmacy.pro.ui.home
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.FacilityProfile
@Composable fun HomeScreen(container:AppContainer,profile:FacilityProfile){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){ElevatedCard{Column(Modifier.padding(18.dp)){Text("الجدول اليومي",style=MaterialTheme.typography.headlineSmall);Text(profile.arabicName);Text("تنظيم زيارات المندوبين حسب الفترة والمنشأة")}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){Card(Modifier.weight(1f)){Column(Modifier.padding(16.dp)){Text("الصباحية");Text("لا توجد زيارات")}};Card(Modifier.weight(1f)){Column(Modifier.padding(16.dp)){Text("المسائية");Text("لا توجد زيارات")}}};Button(onClick={},Modifier.fillMaxWidth()){Text("طباعة إيصال Bluetooth")};OutlinedButton(onClick={},Modifier.fillMaxWidth()){Text("مشاركة itinerary عبر WhatsApp")}}}

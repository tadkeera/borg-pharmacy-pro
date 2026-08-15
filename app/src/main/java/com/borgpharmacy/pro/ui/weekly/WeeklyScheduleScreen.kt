package com.borgpharmacy.pro.ui.weekly
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.*
@Composable fun WeeklyScheduleScreen(container:AppContainer,profile:FacilityProfile){var week by remember{mutableIntStateOf(1)};Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("الجدول الأسبوعي",style=MaterialTheme.typography.headlineSmall);ScrollableTabRow(week){(1..profile.policy.visits).forEach{i->Tab(week==i,{week=i},text={Text("الأسبوع $i")})}};repeat(5){day->ElevatedCard{Column(Modifier.padding(14.dp)){Text(listOf("السبت","الأحد","الإثنين","الثلاثاء","الأربعاء")[day],style=MaterialTheme.typography.titleMedium);Text("زيارات منظمة حسب سياسة ${profile.policy.visits} زيارات")}}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({}){Text("PDF")};Button({}){Text("CSV")};Button({}){Text("HTML")}}}}

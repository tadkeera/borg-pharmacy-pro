package com.borgpharmacy.pro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.FacilityProfile
import com.borgpharmacy.pro.ui.companies.*
import com.borgpharmacy.pro.ui.dashboard.*
import com.borgpharmacy.pro.ui.home.*
import com.borgpharmacy.pro.ui.settings.*
import com.borgpharmacy.pro.ui.weekly.*

data class NavItem(val label:String,val icon:androidx.compose.ui.graphics.vector.ImageVector)
@Composable fun AppNavigation(container:AppContainer, profile:FacilityProfile){
 var selected by remember { mutableIntStateOf(0) }
 val items=listOf(NavItem("اليوم",Icons.Default.Today),NavItem("الأسابيع",Icons.Default.CalendarMonth),NavItem("الشركات",Icons.Default.Business),NavItem("التقارير",Icons.Default.Assessment),NavItem("الإعدادات",Icons.Default.Settings))
 Scaffold(topBar={TopAppBar(title={Text(profile.arabicName)},subtitle={Text(profile.englishName)})},bottomBar={NavigationBar{items.forEachIndexed{i,item->NavigationBarItem(selected==i,{selected=i},icon={Icon(item.icon,item.label)},label={Text(item.label)})}}}){padding->Surface(Modifier.padding(padding)){when(selected){0->HomeScreen(container,profile);1->WeeklyScheduleScreen(container,profile);2->CompaniesScreen(container,profile);3->DashboardScreen(container,profile);else->SettingsScreen(container,profile)}}}
}

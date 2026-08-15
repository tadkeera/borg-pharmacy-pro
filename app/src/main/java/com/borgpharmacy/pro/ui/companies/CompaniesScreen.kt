package com.borgpharmacy.pro.ui.companies
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.*
@Composable fun CompaniesScreen(container:AppContainer,profile:FacilityProfile){var query by remember{mutableStateOf("")};Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("الشركات والمندوبون",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("بحث")});Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({}){Text("إضافة شركة")};OutlinedButton({}){Text("استيراد CSV")}};listOf("الشركات النشطة","المندوبون","نقل مندوب").forEach{label->ElevatedCard{ListItem(headlineContent={Text(label)},supportingContent={Text("إدارة البيانات والعمل دون اتصال")},trailingContent={Text("›")})}}}}

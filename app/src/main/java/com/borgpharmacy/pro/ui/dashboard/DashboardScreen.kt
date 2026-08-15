package com.borgpharmacy.pro.ui.dashboard
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.FacilityProfile
@Composable fun DashboardScreen(container:AppContainer,profile:FacilityProfile){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("التقارير والإحصائيات",style=MaterialTheme.typography.headlineSmall);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("الزيارات المتوقعة" to "0","المكتملة" to "0","نسبة الالتزام" to "—").forEach{(a,b)->ElevatedCard(Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text(a);Text(b,style=MaterialTheme.typography.titleLarge)}}}};ElevatedCard{ListItem(headlineContent={Text("معدل التزام الشركات")},supportingContent={Text("سيظهر بعد تسجيل الزيارات")})};ElevatedCard{ListItem(headlineContent={Text("توزيع الفترات")},supportingContent={Text("صباحية 0  •  مسائية 0")})}}}

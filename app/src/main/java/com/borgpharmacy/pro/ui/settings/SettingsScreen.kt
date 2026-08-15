package com.borgpharmacy.pro.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.*
@Composable fun SettingsScreen(container:AppContainer,profile:FacilityProfile){var confirm by remember{mutableStateOf(false)};Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("الإعدادات",style=MaterialTheme.typography.headlineSmall);ElevatedCard{ListItem(headlineContent={Text("الهوية والعلامة التجارية")},supportingContent={Text(profile.arabicName+" • "+profile.englishName)},trailingContent={Text("تعديل")})};ElevatedCard{Column(Modifier.padding(16.dp)){Text("سياسة الزيارات: ${profile.policy.visits} زيارة");Button({confirm=true}){Text("تغيير السياسة")}}};OutlinedButton({}){Text("نسخة احتياطية محلية")};OutlinedButton({}){Text("استعادة النسخة")};OutlinedButton({}){Text("مزامنة Supabase")}};if(confirm)AlertDialog(onDismissRequest={confirm=false},confirmButton={TextButton({confirm=false}){Text("تأكيد")}},dismissButton={TextButton({confirm=false}){Text("إلغاء")}},title={Text("تعديل سياسة الزيارات")},text={Text("سيتم الحفاظ على الأسابيع الحالية وإضافة أو حذف الأسابيع المطلوبة بشكل غير تدميري.")})}

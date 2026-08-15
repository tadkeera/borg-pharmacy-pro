package com.borgpharmacy.pro
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.borgpharmacy.pro.ui.theme.BorgPharmacyProTheme
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{BorgPharmacyProTheme{}}}}

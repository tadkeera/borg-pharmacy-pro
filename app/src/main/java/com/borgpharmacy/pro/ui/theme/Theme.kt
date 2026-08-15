package com.borgpharmacy.pro.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
@Composable fun BorgPharmacyProTheme(content: @Composable () -> Unit){MaterialTheme(colorScheme=lightColorScheme(primary=BorgBlue,secondary=BorgTeal),typography=BorgTypography,content=content)}

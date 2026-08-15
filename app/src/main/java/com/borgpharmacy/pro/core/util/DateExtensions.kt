package com.borgpharmacy.pro.core.util
import java.time.LocalDate
fun LocalDate.isWorkingDay()=dayOfWeek.value in 1..5

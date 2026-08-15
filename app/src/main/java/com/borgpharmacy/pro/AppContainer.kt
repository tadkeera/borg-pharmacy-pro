package com.borgpharmacy.pro

import android.content.Context
import androidx.room.Room
import com.borgpharmacy.pro.core.database.BorgProDatabase
import com.borgpharmacy.pro.core.printer.BluetoothPrinterManager
import com.borgpharmacy.pro.core.printer.ThermalReceiptCanvas
import com.borgpharmacy.pro.data.repository.OfflineFirstBorgRepository
import com.borgpharmacy.pro.data.repository.OfflineFirstFacilityRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine

class AppContainer(context: Context) {
    val database: BorgProDatabase = Room.databaseBuilder(
        context.applicationContext, BorgProDatabase::class.java, BorgProDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()
    val scheduleEngine = DynamicScheduleEngine()
    val facilityRepository = OfflineFirstFacilityRepository(database.facilityDao())
    val borgRepository = OfflineFirstBorgRepository(database.companyDao(), database.visitDao(), scheduleEngine)
    val receiptCanvas = ThermalReceiptCanvas()
    val bluetoothPrinter = BluetoothPrinterManager()
}

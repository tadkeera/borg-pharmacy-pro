package com.borgpharmacy.pro

import android.content.Context
import androidx.room.Room
import com.borgpharmacy.pro.core.database.BorgProDatabase
import com.borgpharmacy.pro.core.database.MIGRATION_1_2
import com.borgpharmacy.pro.data.local.RoomBorgLocalDataSource
import com.borgpharmacy.pro.data.remote.SupabaseRemoteDataSource
import com.borgpharmacy.pro.core.printer.BluetoothPrinterManager
import com.borgpharmacy.pro.core.security.SecureSessionStore
import com.borgpharmacy.pro.core.sync.SyncManager
import com.borgpharmacy.pro.core.printer.ThermalReceiptCanvas
import com.borgpharmacy.pro.data.repository.OfflineFirstBorgRepository
import com.borgpharmacy.pro.data.repository.OfflineFirstFacilityRepository
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import com.borgpharmacy.pro.domain.usecase.ObserveCompaniesUseCase
import com.borgpharmacy.pro.domain.usecase.ObserveVisitsUseCase
import com.borgpharmacy.pro.domain.usecase.ReconcileCompanyScheduleUseCase

class AppContainer(context: Context) {
    val secureSessionStore = SecureSessionStore(context.applicationContext)
    val database: BorgProDatabase = Room.databaseBuilder(
        context.applicationContext,
        BorgProDatabase::class.java,
        BorgProDatabase.DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2).build()
    val scheduleEngine = DynamicScheduleEngine()
    val facilityRepository = OfflineFirstFacilityRepository(database.facilityDao())
    val localDataSource = RoomBorgLocalDataSource(
        companyDao = database.companyDao(),
        visitDao = database.visitDao(),
        syncQueueDao = database.syncQueueDao(),
    )
    val borgRepository = OfflineFirstBorgRepository(
        localDataSource = localDataSource,
        engine = scheduleEngine,
    )
    val observeCompanies = ObserveCompaniesUseCase(borgRepository)
    val observeVisits = ObserveVisitsUseCase(borgRepository)
    val reconcileCompanySchedule = ReconcileCompanyScheduleUseCase(borgRepository)
    val syncManager = SyncManager(
        queueDao = database.syncQueueDao(),
        sessionStore = secureSessionStore,
        syncService = SupabaseRemoteDataSource(),
    )
    val receiptCanvas = ThermalReceiptCanvas()
    val bluetoothPrinter = BluetoothPrinterManager()
}

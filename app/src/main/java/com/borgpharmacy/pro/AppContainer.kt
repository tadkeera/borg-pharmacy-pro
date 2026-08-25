package com.borgpharmacy.pro
import android.content.Context
import androidx.room.Room
import com.borgpharmacy.pro.core.database.*
import com.borgpharmacy.pro.core.audit.AuditLogRepository
import com.borgpharmacy.pro.core.audit.UserAuditService
import com.borgpharmacy.pro.core.network.*
import com.borgpharmacy.pro.core.printer.*
import com.borgpharmacy.pro.data.repository.*
import com.borgpharmacy.pro.domain.scheduler.DynamicScheduleEngine
import com.borgpharmacy.pro.domain.sync.OutboxWriter
class AppContainer(context:Context){
 val database:BorgProDatabase=Room.databaseBuilder(context.applicationContext,BorgProDatabase::class.java,BorgProDatabase.DATABASE_NAME).addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4,MIGRATION_4_5,MIGRATION_5_6).build()
 val auditLogRepository=AuditLogRepository(database.auditLogDao())
 val userAuditService=UserAuditService(auditLogRepository)
 val backupManager=LocalBackupManager(context,auditLogRepository)
 val scheduleEngine=DynamicScheduleEngine()
 val facilityRepository=OfflineFirstFacilityRepository(database.facilityDao())
 val borgRepository=OfflineFirstBorgRepository(database.companyDao(),database.visitDao(),scheduleEngine)
 val receiptCanvas=ThermalReceiptCanvas();val bluetoothPrinter=BluetoothPrinterManager();val authRepository=SupabaseAuthRepository(context)
 val syncEngine=SyncEngine(database.syncDao(),database.syncMetadataDao(),PullReconciler(database),auditLogRepository)
 val outboxWriter=OutboxWriter(database.syncDao(),context)
 val mutationRepository=OfflineFirstMutationRepository(this,outboxWriter)
 val extendedMutationRepository=OfflineFirstExtendedMutationRepository(this,outboxWriter)
 val userMutationRepository=UserMutationRepository(this)
}

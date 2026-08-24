# Phase 3.0 Foundation Test Manifest

Canonical inventory for the Phase 3.0 Foundation CI gate. The GitHub repository checkout is authoritative.

| Purpose | File | Package | Class | Source set | CI execution |
|---|---|---|---|---|---|
| Authorization/permit/employee foundation associations, tenant and slot invariants | `app/src/test/java/com/borgpharmacy/pro/phase3/Phase3FoundationE2ETest.kt` | `com.borgpharmacy.pro.phase3` | `Phase3FoundationE2ETest` | Unit | `:app:testDebugUnitTest --tests com.borgpharmacy.pro.phase3.Phase3FoundationE2ETest` |
| Room foundation database/instrumentation | `app/src/androidTest/java/com/borgpharmacy/pro/FoundationDatabaseTest.kt` | `com.borgpharmacy.pro` | `FoundationDatabaseTest` | Instrumentation | `connectedDebugAndroidTest` |
| Room 5 -> 6 migration acceptance | `app/src/androidTest/java/com/borgpharmacy/pro/RoomMigrationTest.kt` | `com.borgpharmacy.pro` | `RoomMigrationTest` | Instrumentation | `connectedDebugAndroidTest` |
| Room v4 migration acceptance helper | `app/src/androidTest/java/com/borgpharmacy/pro/core/database/RoomV4MigrationAcceptanceTest.kt` | `com.borgpharmacy.pro.core.database` | `RoomV4MigrationAcceptanceTest` | Instrumentation | `connectedDebugAndroidTest` |

The following names are intentionally not workflow filters because they are absent from the GitHub checkout at the canonical Phase 3.0 gate commit:

- `Phase3SyncAcceptanceTest`
- `Phase3AuditAcceptanceTest`
- `ReceptionPermitWorkflowE2ETest`
- `Phase3RbacAcceptanceTest`
- `Phase3DatabaseAcceptanceTest`
- `Phase3FoundationFinalE2ETest`
- `Phase3PermitPrinterTest` (no verified Phase 3 instrumentation file in the checkout)

No approval may be issued for gates not represented by an actual file and executed command.

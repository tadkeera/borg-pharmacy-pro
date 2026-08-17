# Phase 2 Completion Report

## Scope

Phase 2 consolidated the active Android architecture and completed the production synchronization pipeline without redesigning the UI or adding Product/Transaction commercial features.

## Architecture before and after

| Concern | Before | After |
|---|---|---|
| Launcher | Manifest launched `com.borgpharmacy.pro`, but the old `com.borgpharmacy` tree was still compiled. | Only `com.borgpharmacy.pro` remains in the Android source set and is the launcher architecture. |
| Domain | Repository interface was minimal and validation was implicit. | Explicit `ObserveCompaniesUseCase`, `ObserveVisitsUseCase`, `ReconcileCompanyScheduleUseCase`, and `DomainValidators` sit between UI and data. |
| Local data | Repository accessed Room DAOs directly. | `BorgLocalDataSource` and `RoomBorgLocalDataSource` isolate Room details from the repository. |
| Remote data | Active pro code imported the legacy `SupabaseSyncService`. | `SupabaseRemoteDataSource` and `SyncRemoteDataSource` are self-contained in pro and expose only Auth/Profile/Secure Sync contracts. |
| Sync | Empty `SupabaseSyncEngine` and intake-only secure-sync function. | `SyncManager` processes bounded queue batches, refreshes sessions, sends authenticated operations, applies outcomes, retries temporary failures, and terminally marks conflicts/rejections. Server RPC applies domain rows atomically with version checks and audit history. |

## Deleted legacy files

The following old Android source tree was removed after the migration report and active-entry-point checks:

| Deleted area | Reason |
|---|---|
| `com/borgpharmacy/MainActivity.kt`, `BorgPharmacyApplication.kt` | Not referenced by Manifest. |
| Legacy `backup`, `communications`, `print`, and `security` classes | Not reachable from the active pro launcher/navigation. |
| Legacy `data/local`, `data/remote`, and `data/repository` | Duplicate Room, remote, and repository implementations; pro now has its own data boundaries. |
| Legacy `domain` and `ui` classes | Duplicate inactive architecture. |
| Empty `pro/core/network/SupabaseSyncEngine.kt` | Replaced by production `core/sync/SyncManager.kt`. |

No Supabase migration or backend function was deleted. All removed source remains recoverable in Git history.

## Migrated and added files

The remote contract moved to `app/src/main/java/com/borgpharmacy/pro/data/remote/SupabaseRemoteDataSource.kt`. Domain boundaries were added under `domain/usecase`, `domain/validation`, and `domain/sync`. Local data boundaries were added under `data/local`. `SyncManager`, `SyncWorker`, encrypted session storage, and the pro AppContainer now use these contracts.

The server gained `2026_08_phase2_sync.sql`, which adds `sync_version`, `sync_audit`, and `apply_sync_operations`, and `2026_08_phase2_rls.sql`, which removes permissive bootstrap policies and installs tenant/role-aware policies. `secure-sync/index.ts` now calls the authenticated apply RPC and returns applied, conflict, and rejected outcomes.

## Sync design

The production path is now:

```text
Local mutation
  -> Room entity with tenantId/syncVersion
  -> tenant-scoped sync_queue with idempotencyKey
  -> WorkManager SyncWorker when connected
  -> authenticated Supabase Edge Function
  -> JWT/profile/role/tenant/payload validation
  -> sync_operations idempotency ledger
  -> apply_sync_operations RPC with row locks
  -> last-write protection using sync_version
  -> domain table update or CONFLICT/REJECTED outcome
  -> sync_audit record
  -> Android queue delete, terminal conflict, terminal rejection, or temporary retry
```

The server policy is **last-write protection**: an incoming operation applies only when its version is greater than the stored version. Equal or older versions become conflicts. Cross-tenant entity references and unsupported entity types are rejected. The client also exposes `LAST_WRITE_WINS`, `REJECT_STALE`, and `MANUAL_REVIEW` domain policies for deterministic unit testing and future approved conflict workflows.

## Database changes

Room remains at version 2 with non-destructive migrations and exported schema configuration. The pro entities have tenant/version indexes and a tenant-scoped queue. Android migration instrumentation now checks preservation of existing data and creation of `sync_queue`.

Supabase adds server `sync_version` columns and indexes for Company, Representative, and Visit. `sync_operations` is the idempotency ledger, while `sync_audit` is the immutable operational history. Product and Transaction are not present in the current domain or schema and were intentionally not created because introducing them would be commercial feature development outside this phase.

## Security validation

Static validation passes for the absence of `SUPABASE_SYNC_TOKEN` in the Android client and Gradle properties, absence of `fallbackToDestructiveMigration`, absence of legacy `p_token` transport in Android source, absence of legacy package imports, and clean Git whitespace checks. The RLS matrix is documented in `supabase/tests/phase2_rls_matrix.sql` for execution in a disposable Supabase environment with six role accounts and two tenants.

## Test foundation

Unit tests cover domain validation/use cases, role/session behavior inherited from Phase 1, conflict resolution, and SyncManager queue outcome handling. Android instrumentation tests cover tenant isolation, queue idempotency scope, Room schema version, and a direct 1→2 migration preserving rows. Gradle task configuration succeeds offline.

The Android unit and instrumentation test execution remains blocked in this sandbox because no Android SDK is installed and no `ANDROID_HOME` or valid `local.properties` SDK path exists. Real Supabase RLS and Edge Function integration tests also require a disposable Supabase project with seeded users and tenants; the repository includes the matrix and migration scripts but does not run them against production.

## Remaining risks

The SQL migrations and Edge Function must be deployed and tested in a disposable Supabase project before production. The current server apply function supports Company, Representative, and Visit; User synchronization is deliberately rejected because Auth/profile management must remain in the dedicated admin function. Products and Transactions require a future approved domain design. A manual smoke test is still required for onboarding, schedule reconciliation, printing, backup behavior retained in the product requirements, session restore, and online/offline reconnection.

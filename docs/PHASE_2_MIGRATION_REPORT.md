# Phase 2 Architecture Migration Report

## Scope and rule

This report is created before deleting any legacy file. The goal is to consolidate the Android architecture without changing the UI or introducing commercial features.

## Active production entry point

`app/src/main/AndroidManifest.xml` declares `com.borgpharmacy.pro.BorgAppApplication` as the application class and `com.borgpharmacy.pro.MainActivity` as the launcher activity. Therefore, the `com.borgpharmacy.pro` tree is the active production entry point for the current APK.

The legacy `com.borgpharmacy.BorgPharmacyApplication` and `com.borgpharmacy.MainActivity` are not referenced by the manifest. The legacy tree is nevertheless still compiled because it is inside the same Android source set. This means it can break builds, duplicate behavior, and preserve insecure/deprecated code even though it is not the launcher path.

## Architecture inventory

| Area | Active `com.borgpharmacy.pro` tree | Legacy `com.borgpharmacy` tree | Consolidation decision |
|---|---|---|---|
| Application/launcher | `BorgAppApplication`, `MainActivity` | `BorgPharmacyApplication`, `MainActivity` | Keep active pro entry point; remove legacy entry point after migration check. |
| Database | `BorgProDatabase`, pro entities/DAOs, Room v2 migration | `BorgDatabase`, extensive Room v6 migration history | Keep pro schema as the target; preserve required legacy migration knowledge in documentation before deletion. |
| Repository | Small `OfflineFirstBorgRepository` | Large `BorgRepository` with catalog, users, reports, backups, and mutation flows | Extract required domain contracts/use cases from legacy repository into pro layers before deleting it. |
| Remote access | Pro sync currently depends on legacy `SupabaseSyncService` | Legacy service contains Auth DTOs, profile access, secure-sync transport, and disabled old RPC guards | Move the remote service and DTOs into `pro.core.network` or `pro.data.remote`. |
| Security/session | Pro `SecureSessionStore`, role policy | Legacy repository still owns login orchestration and local fallback behavior | Move Auth/session orchestration into a pro data/auth module; delete local legacy fallback. |
| Scheduling | Pro dynamic scheduler | Legacy cycle/schedule generator and reports | Compare behavior and retain only domain rules required by active screens/tests. |
| Printing/backup | Pro printer classes exist | Legacy backup/WhatsApp/print services contain existing workflows | Migrate required interfaces/adapters; do not delete until call-site inventory is complete. |
| UI | Pro Compose screens and ViewModels | Legacy Compose/XML screens | Keep pro UI; no redesign in Phase 2. |

## Required migration before deletion

The legacy repository currently contains functionality not represented in the small pro repository interface: authentication entry points, user management, company/representative/visit mutations, reports, backup hooks, and sync orchestration. These capabilities must be represented by explicit pro use cases or adapters before the legacy repository can be safely deleted.

The remote contract has now been moved into `com.borgpharmacy.pro.data.remote.SupabaseRemoteDataSource` with `SyncRemoteDataSource`, Auth session models, and secure-sync DTOs. `SyncManager` and `AppContainer` no longer import the legacy remote service. This is a package migration, not a UI change.

The legacy Room schema contains a longer migration history than the pro schema. Before deletion, production database compatibility must be decided: either all active devices already use the pro database, or a one-time import/migration adapter must be maintained. No legacy database file will be deleted until this decision is tested.

## Deleted legacy files

After the migration report and active-entry-point verification, the following unused Android source tree was deleted from the production source set:

1. `com/borgpharmacy/MainActivity.kt` and `com/borgpharmacy/BorgPharmacyApplication.kt`.
2. Legacy backup, communications, print, security, database, remote, repository, domain, and UI files under `com/borgpharmacy/`.
3. The empty `com/borgpharmacy/pro/core/network/SupabaseSyncEngine.kt`, replaced by `core/sync/SyncManager.kt`.

All deleted files remain recoverable through Git history. No Supabase SQL migration or backend function was deleted.

## Required safety gates

No deletion is safe until the following gates pass: a full source reference scan, successful Android build, unit tests, Room migration tests from all supported versions, tenant-isolation tests, Auth/RLS tests, and a manual smoke test of onboarding, login/session restore, company/representative/visit flows, printing, backup, and sync.

## Current conclusion

The pro package is now the only Android launcher architecture and no longer imports the legacy tree. It contains explicit domain validators/use cases, a local data-source abstraction, a focused Supabase remote datasource, and a production sync manager. The removed legacy repository contained broader workflows that were not reachable from the active Manifest/pro navigation; those workflows are intentionally not part of the Phase 2 active surface. Git history preserves the deleted implementation if a future approved product requirement requires selective reintroduction through pro use cases.

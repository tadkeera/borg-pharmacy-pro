# Foundation Hardening

This document records the Phase 1 foundation changes applied to Borg Pharmacy Pro. No commercial features or UI redesign work was added.

## Before and after architecture

| Area | Before | After |
|---|---|---|
| Client writes | Legacy Android RPC calls used a shared `SUPABASE_SYNC_TOKEN` embedded through `BuildConfig`. | Client-side legacy writes fail closed. New writes target the authenticated `secure-sync` Edge Function contract. |
| Authentication | Access and refresh tokens were stored in Room settings without expiry handling. | Auth sessions use encrypted Android preferences, explicit expiry timestamps, refresh handling, and fail-closed logout. |
| Authorization | Mostly client-side/admin checks and limited role representation. | Six-role domain model plus server-side role/tenant checks in SQL/RLS and Edge Functions. |
| Local database | Room version 1 with `fallbackToDestructiveMigration()`. | Room version 2, non-destructive `MIGRATION_1_2`, synchronization metadata, and a tenant-scoped queue. |
| Tenant isolation | Reconciliation passed an empty tenant ID and some queries were unbounded. | Tenant is required for reconciliation, DAO reads/deletes are tenant-scoped, and list/page queries have limits. |
| Synchronization | `SupabaseSyncEngine` was empty. | Queue entities, idempotency keys, bounded batches, retry/backoff, authenticated transport, and WorkManager scheduling are present. |
| CI safety | No invariant scan for the critical security regressions. | CI scans for client secrets, destructive migration fallback, and legacy `p_token` usage. |

## Security improvements

The Android client no longer defines or references `SUPABASE_SYNC_TOKEN`. The legacy RPC transport and all methods that depended on the shared token fail closed, while the server migration revokes execution of the old token-based RPCs. The Android manifest also disables cleartext traffic.

`SecureSessionStore` uses Android encrypted preferences for access tokens, refresh tokens, tenant identity, and expiry. The legacy repository was wired to this store and clears obsolete Room token keys during logout. Supabase Auth sessions now calculate an absolute expiry time and refresh when the session enters a 60-second safety window.

The server foundation migration introduces the six roles `OWNER`, `ADMIN`, `PHARMACIST`, `EMPLOYEE`, `REPRESENTATIVE`, and `VIEWER`, together with role helper functions, tenant-scoped RLS for `sync_operations`, and an idempotency constraint. The `secure-sync` Edge Function verifies the bearer token, active profile, tenant, role, operation, entity type, payload, version, and idempotency key. The user-creation Edge Function now validates roles and requires a 12-character password minimum.

## Database improvements

Room version 2 adds synchronization metadata to the main entities and creates `sync_queue`. `MIGRATION_1_2` uses `ALTER TABLE`, creates indexes, and does not delete existing records. The application no longer calls `fallbackToDestructiveMigration()`.

DAO queries are tenant-scoped and bounded. Companies and representatives have page queries, visits have bounded list/page queries, and visit soft-delete requires a tenant ID. The queue has tenant-aware retry state, status, timestamps, version information, and a composite tenant/idempotency uniqueness constraint.

## Synchronization improvements

`SyncManager` reads only pending rows for the requested tenant, refreshes an expired Auth session, submits a maximum of 100 operations through the authenticated Edge Function, removes accepted operations, and applies exponential backoff for failures. `SyncWorker` runs only with network connectivity and is scheduled as unique periodic work. Reconciliation now writes tenant-scoped visit records and corresponding queue operations before returning.

## Tests created

Unit tests cover role parsing, role policy boundaries, and the session expiration safety window. Android instrumentation tests cover tenant-filtered DAO reads, tenant-scoped idempotency keys, and the Room version 2 schema. CI invariants prevent reintroduction of client-side shared-token references, destructive migration fallback, and `p_token` in Android source.

## Verification status

Gradle task configuration succeeds offline. Static invariant scans and `git diff --check` pass. The Android unit test task cannot run in the current sandbox because no Android SDK is installed and no `ANDROID_HOME` or `local.properties` SDK path is available. Instrumentation and migration execution therefore remain pending in GitHub Actions or another Android SDK-enabled environment.

## Remaining risks

The Supabase SQL migration and Edge Functions still require deployment to the real project and an RLS matrix test using real `OWNER`, `ADMIN`, `PHARMACIST`, `EMPLOYEE`, `REPRESENTATIVE`, and `VIEWER` accounts. The secure-sync function currently records accepted operations in `sync_operations`; domain-table application and conflict resolution policy must be completed and tested against the production schema before go-live.

The active and legacy Android source trees still coexist and should be consolidated after this foundation baseline is accepted. The broad storage and Bluetooth permissions remain for existing backup/printer behavior and require a separate Google Play/storage-access review. No production release should be signed until Android build, migration, RLS, offline, retry, and restore tests pass in CI.

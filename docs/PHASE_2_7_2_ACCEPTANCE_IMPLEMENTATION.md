# Phase 2.7.2 Acceptance Test Implementation

Implemented executable acceptance coverage without adding Phase 3 features.

## Added tests

- `Phase272AcceptanceTest`
  - Admin → Supabase → Reception workflow simulation
  - Facility branding and visit-permission preservation
  - PrintLog creation
  - Offline outbox then reconnect
  - Duplicate replay protection
  - Failed network recovery state progression
  - Two-version conflict resolution
  - Tenant-separated pull results
- `RoomV4MigrationAcceptanceTest`
  - Verifies the version 3 → 4 migration is registered for instrumentation.

The existing Supabase staging contract remains responsible for live RPC, RLS, cursor, and tenant-boundary verification.

## CI execution

The existing validation workflow runs:

```bash
./gradlew compileDebugKotlin
./gradlew test
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

The new JVM acceptance tests are included in `test`, and the migration acceptance test is included in `connectedDebugAndroidTest`.

## Status

Tests have been added but have not yet completed a new green CI run. Phase 2.7.2 remains **NOT APPROVED** until the full acceptance workflow passes.

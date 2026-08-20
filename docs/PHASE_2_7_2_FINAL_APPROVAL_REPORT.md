# Phase 2.7.2 Final Stabilization and Approval Report

## Status

**Phase 2.7.2: APPROVED**

Phase 3 remains out of scope and must not begin without separate approval.

## CI verification

Workflow URL:

https://github.com/tadkeera/borg-pharmacy-pro/actions/runs/32419598703

Commit tested:

```text
c30a08154eb106423707260b7284e1d8a8fb2dbd
```

Final workflow result:

```text
success
```

## Passed jobs

### Compile, unit tests, and debug APK

Passed:

```bash
./gradlew compileDebugKotlin
./gradlew test
./gradlew assembleDebug
```

### Android instrumentation and Room migration

Passed:

```bash
./gradlew connectedDebugAndroidTest
```

The Android instrumentation job completed successfully, including the Room migration acceptance coverage.

### Supabase staging validation

Passed:

- Baseline staging schema setup
- Phase 2.7.1 security migration
- Deterministic Phase 2.7.2 synchronization bootstrap
- Existing sync constraint widening
- `sync_push` and `sync_pull` availability
- FacilityProfile and PrintLog entity support
- Cursor synchronization contract
- Duplicate replay protection
- Tenant A/B fixture setup
- Tenant isolation validation
- Sync verification logs and cleanup

## Final stabilization changes verified

The deterministic bootstrap migration now safely recreates or updates:

- `sync_schema_version`
- `sync_operations`
- `sync_changes`
- `facility_profiles`
- `print_logs`
- Synchronization indexes
- Synchronization RLS policies
- `sync_push(jsonb)`
- `sync_pull(cursor, limit)`
- `sync_push_test(entity)`

Supported sync entity types:

```text
COMPANY
REPRESENTATIVE
VISIT
FACILITY_PROFILE
PRINT_LOG
```

The existing `sync_operations_entity_type_check` constraint is widened idempotently so existing staging databases do not reject FacilityProfile or PrintLog operations.

## Acceptance summary

| Gate | Result |
|---|---|
| Gradle compilation | PASS |
| Unit tests | PASS |
| Debug APK assembly | PASS |
| Android instrumentation | PASS |
| Room migration acceptance | PASS |
| Supabase migration | PASS |
| Sync schema bootstrap | PASS |
| Sync contract | PASS |
| Cursor validation | PASS |
| Duplicate replay protection | PASS |
| Tenant A/B isolation | PASS |
| FacilityProfile sync support | PASS |
| PrintLog sync support | PASS |

## Remaining risks

- Production Supabase RPC observability and alerting should continue to be monitored.
- Sync payload schemas must remain versioned as future entity fields are added.
- Real-device Bluetooth printer testing remains operational testing rather than a CI gate.
- Release signing, staged rollout, backup, and incident-response procedures remain separate deployment concerns.

## Approval decision

The Phase 2.7.2 stabilization workflow completed green. Phase 2.7.2 is approved for closure.

No Phase 3 features were implemented as part of this phase.

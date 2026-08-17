-- Phase 2.5 staging sync matrix.
-- Execute only in a disposable Supabase environment with two tenants and six users.
-- The Android harness supplies the operation UUIDs and tokens; this file records
-- the assertions that must be checked after each scenario.

begin;

-- Scenario 1: Device A creates a Company while offline.
-- Expected after reconnect: one sync_operations row, one APPLIED audit row,
-- company tenant_id = Tenant A, and queue entry deleted on the device.

-- Scenario 2: Device B submits an older version for the same entity.
-- Expected: no domain overwrite, outcome = CONFLICT, audit current_version > incoming_version.

-- Scenario 3: Device B submits a newer version.
-- Expected: domain row changes, sync_version becomes the incoming version,
-- outcome = APPLIED, and exactly one audit row exists for the operation.

-- Scenario 4: replay the same idempotency key.
-- Expected: no duplicate domain change and no duplicate audit row.

-- Scenario 5: Tenant B submits an operation whose company_id belongs to Tenant A.
-- Expected: REJECTED, no domain row written, and no cross-tenant data exposure.

-- Scenario 6: EMPLOYEE, REPRESENTATIVE, and VIEWER call secure-sync.
-- Expected: HTTP 403 and no sync_operations rows created.

-- Scenario 7: temporary network failure on Android.
-- Expected: queue remains PENDING with incremented attempts and bounded nextAttemptAt.

-- Scenario 8: permanent CONFLICT or REJECTED response.
-- Expected: queue status becomes CONFLICT or FAILED with no infinite retry loop.

rollback;

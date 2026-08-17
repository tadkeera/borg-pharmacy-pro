# Phase 2.5 Production Validation Report

## Approval status

**Phase 3 approval: BLOCKED / NOT APPROVED.** Local structural validation passed, but production validation cannot be approved until the Supabase staging matrix and Android real-device tests run successfully.

## Environment assessment

| Capability | Result | Evidence |
|---|---|---|
| Repository state | Passed | Clean working tree before Phase 2.5; baseline commit `78c4077`. |
| Gradle configuration | Passed | `./gradlew :app:tasks --all --offline` exposes debug, staging, release, and test tasks. |
| Android SDK/device | Blocked | No `ANDROID_HOME`, `adb`, emulator, or SDK path is available. |
| Supabase CLI/Postgres/Docker | Blocked | `supabase`, `psql`, and `docker` are not installed. |
| Supabase credentials | Blocked | No staging URL, anon key, service-role key, or test user tokens are configured. |
| Edge Function syntax | Not run | Deno is not installed. |

## Local checks executed

The local harness `scripts/phase25_validation.sh` passed the secret scan for Android/Gradle, destructive migration scan, legacy architecture scan, Gradle configuration check, and repository whitespace validation. The check reports the Edge Function syntax step as skipped because Deno is unavailable.

The Supabase smoke harness `scripts/phase25_supabase_smoke.sh` is executable and correctly fails closed with exit code `2` when the staging environment is not configured. It does not invent credentials or contact production.

The staging and release assemble tasks were attempted. Both were blocked before compilation by the missing Android SDK and `local.properties`/`ANDROID_HOME` configuration. No APK was produced.

## Supabase validation plan

Create a disposable Supabase project and apply the SQL files in this order:

1. Existing schema and prerequisite migrations.
2. `2026_08_foundation_security.sql`.
3. `2026_08_phase2_sync.sql`.
4. `2026_08_phase2_rls.sql`.

Seed Tenant A and Tenant B, then create one active Auth/profile user for each role: `OWNER`, `ADMIN`, `PHARMACIST`, `EMPLOYEE`, `REPRESENTATIVE`, and `VIEWER`. Configure the six test access tokens and tenant UUIDs as environment variables, then run `scripts/phase25_supabase_smoke.sh`. Execute `supabase/tests/phase2_rls_matrix.sql` and `supabase/tests/phase25_sync_matrix.sql` in the disposable environment.

The required assertions are that every role sees only its own tenant, only OWNER/ADMIN/PHARMACIST can write approved catalog/schedule data, lower roles receive HTTP 403 for secure synchronization, older versions cannot overwrite newer rows, replayed idempotency keys do not duplicate audit history, cross-tenant references are rejected, and applied/conflict/rejected outcomes match Android queue cleanup behavior.

## Android validation plan

With Android SDK and a connected test device or emulator, configure `local.properties` and run `./gradlew testStagingUnitTest`, `./gradlew connectedStagingAndroidTest`, and `./gradlew assembleStaging`. Install the staging APK and execute fresh install, login, session restore, logout, token refresh, offline queue creation, reconnection, conflict handling, and migration-upgrade checks. Only after staging passes should a signed release build be produced with production-only credentials supplied through protected CI secrets.

## Failed or blocked tests

The Supabase smoke test was not executed because no staging environment variables are configured. Edge Function syntax validation was skipped because Deno is absent. Staging and release APK assembly failed before compilation because Android SDK location is unavailable. No real-device, RLS, RPC, audit, or online/offline synchronization test has therefore been certified in this session.

## Fixed issues during validation

A false-positive secret scan in the Phase 2.5 harness was corrected so that it scans Android source and Gradle properties rather than the intentional GitHub Actions secret name. A dedicated `staging` build type was added with an independent application ID and separate `SUPABASE_STAGING_URL` / `SUPABASE_STAGING_ANON_KEY` inputs. No production credential is used as a staging fallback.

## Production risks

The migrations and Edge Function have not yet been applied to a disposable Supabase project. SQL execution and RLS behavior remain unverified against the live PostgreSQL engine. The Android binary, WorkManager behavior, encrypted session restore, token refresh, Room migration, queue cleanup, and conflict audit flow remain unverified on a real device. Phase 3 must not begin until these gates pass and the failed checks are rerun successfully.

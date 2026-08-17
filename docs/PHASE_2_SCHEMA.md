# Phase 2 Schema Documentation

## Current mobile entities

| Entity | Current Room table | Tenant key | Version field | Index coverage | Phase 2 status |
|---|---|---|---|---|---|
| Facility profile | `facility_profiles` | `tenantId` | `syncVersion` | Primary key; tenant is part of the record | Retained as local configuration. |
| Company | `companies` | `tenantId` | `syncVersion` | Tenant, tenant/name, tenant/updatedAt | Server sync version and tenant RLS added. |
| Representative | `representatives` | `tenantId` | `syncVersion` | Tenant/company, tenant/name, tenant/updatedAt | Server FK and tenant-scoped sync path added. |
| Visit | `visits` | `tenantId` | `syncVersion` | Tenant/cycle, tenant/date/shift/slot, tenant/updatedAt | Server version protection and queue payload finalized. |
| User | `users` | `tenantId` | `syncVersion` | Tenant/username unique, tenant/updatedAt | Auth profile remains authoritative; local entity is not a source of server authorization. |
| Product | Not present in current Room or Supabase schema | Not applicable | Not applicable | Not applicable | Intentionally not created in Phase 2 because this would be a commercial feature/domain expansion. |
| Transaction | Not present in current Room or Supabase schema | Not applicable | Not applicable | Not applicable | Intentionally not created in Phase 2 because this would be a commercial feature/domain expansion. |

## Server-side sync entities

`sync_operations` is the authenticated intake and idempotency ledger. Its uniqueness constraint is `(tenant_id, idempotency_key)`, so retries are safe within a tenant without causing cross-tenant key collisions.

`sync_audit` records the actor, tenant, entity, incoming version, current version, payload, outcome, and conflict reason. It is readable only for the current tenant through RLS and is not writable by mobile clients.

`companies`, `representatives`, and `visits` receive `sync_version` fields and tenant/version indexes. The `apply_sync_operations` function uses row locks, verifies the actor role and tenant, applies only newer versions, validates foreign tenant membership, and records `APPLIED`, `CONFLICT`, or `REJECTED` outcomes.

## Constraints and policies

The target RLS policy is that all authenticated roles may read active rows from their own tenant, only `OWNER`, `ADMIN`, and `PHARMACIST` may write catalog/schedule rows, and only `OWNER`/`ADMIN` may manage profiles. `EMPLOYEE`, `REPRESENTATIVE`, and `VIEWER` are read-oriented roles and cannot synchronize writes.

The Phase 2 SQL files are ordered after the existing foundation migration:

1. `2026_08_foundation_security.sql`
2. `2026_08_phase2_sync.sql`
3. `2026_08_phase2_rls.sql`

They must be applied to a disposable Supabase environment first, followed by the role matrix in `supabase/tests/phase2_rls_matrix.sql`.

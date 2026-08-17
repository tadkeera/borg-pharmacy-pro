-- Phase 2 RLS test matrix.
-- Run this script in a disposable Supabase test project after seeding two tenants and
-- one active user per role. It is intentionally not executed against production.
-- Required variables: :tenant_a, :tenant_b, :user_owner, :user_admin,
-- :user_pharmacist, :user_employee, :user_representative, :user_viewer.

begin;

-- Every role must be able to read only its own tenant.
-- Expected: one row for tenant A, zero rows for tenant B for each role.
-- psql examples:
-- set local role authenticated;
-- select set_config('request.jwt.claim.sub', :'user_owner', true);
-- select count(*) from public.companies where tenant_id = :'tenant_a';
-- select count(*) from public.companies where tenant_id = :'tenant_b';

-- Expected write matrix for companies/representatives/visits:
-- OWNER: INSERT/UPDATE own tenant = allowed; cross-tenant = denied.
-- ADMIN: INSERT/UPDATE own tenant = allowed; cross-tenant = denied.
-- PHARMACIST: INSERT/UPDATE own tenant = allowed; cross-tenant = denied.
-- EMPLOYEE: INSERT/UPDATE = denied; own-tenant SELECT = allowed.
-- REPRESENTATIVE: INSERT/UPDATE = denied; own-tenant SELECT = allowed.
-- VIEWER: INSERT/UPDATE = denied; own-tenant SELECT = allowed.

-- Expected user profile matrix:
-- OWNER and ADMIN may manage profiles in their own tenant only.
-- PHARMACIST, EMPLOYEE, REPRESENTATIVE, and VIEWER may not manage profiles.
-- No role may select profiles outside its current tenant.

-- Expected sync matrix:
-- secure-sync accepts only OWNER, ADMIN, and PHARMACIST.
-- EMPLOYEE, REPRESENTATIVE, and VIEWER receive HTTP 403 for write synchronization.
-- apply_sync_operations is executable only by service_role.

rollback;

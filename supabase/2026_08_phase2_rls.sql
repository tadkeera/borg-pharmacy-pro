-- Phase 2 RLS finalization. Apply after 2026_08_foundation_security.sql.

-- Remove historical policies that were intentionally permissive during bootstrap.
do $$
declare
  policy_row record;
begin
  for policy_row in
    select schemaname, tablename, policyname
    from pg_policies
    where schemaname = 'public'
      and tablename in ('companies', 'representatives', 'visits', 'user_profiles', 'tenants')
  loop
    execute format('drop policy if exists %I on %I.%I', policy_row.policyname, policy_row.schemaname, policy_row.tablename);
  end loop;
end $$;

-- Shared read policy: every authenticated role sees only active rows in its own tenant.
create policy companies_select_tenant
on public.companies for select to authenticated
using (tenant_id = public.current_tenant_id() and coalesce(is_deleted, false) = false);

create policy representatives_select_tenant
on public.representatives for select to authenticated
using (tenant_id = public.current_tenant_id() and coalesce(is_deleted, false) = false);

create policy visits_select_tenant
on public.visits for select to authenticated
using (tenant_id = public.current_tenant_id() and coalesce(is_deleted, false) = false);

-- Catalog writes are limited to OWNER, ADMIN, and PHARMACIST.
create policy companies_write_catalog
on public.companies for insert to authenticated
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

create policy companies_update_catalog
on public.companies for update to authenticated
using (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2())
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

create policy representatives_write_catalog
on public.representatives for insert to authenticated
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

create policy representatives_update_catalog
on public.representatives for update to authenticated
using (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2())
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

create policy visits_write_catalog
on public.visits for insert to authenticated
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

create policy visits_update_catalog
on public.visits for update to authenticated
using (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2())
with check (tenant_id = public.current_tenant_id() and public.can_write_catalog_v2());

-- Profiles: users can read their tenant; only OWNER/ADMIN manage users.
create policy profiles_select_tenant
on public.user_profiles for select to authenticated
using (tenant_id = public.current_tenant_id());

create policy profiles_manage_users
on public.user_profiles for all to authenticated
using (tenant_id = public.current_tenant_id() and public.can_manage_users_v2())
with check (tenant_id = public.current_tenant_id() and public.can_manage_users_v2());

create policy tenants_select_own
on public.tenants for select to authenticated
using (id = public.current_tenant_id());

create policy tenants_owner_update
on public.tenants for update to authenticated
using (id = public.current_tenant_id() and public.current_app_role_v2() = 'OWNER'::public.app_role)
with check (id = public.current_tenant_id() and public.current_app_role_v2() = 'OWNER'::public.app_role);

revoke all on public.companies, public.representatives, public.visits from anon;
grant select, insert, update on public.companies, public.representatives, public.visits to authenticated;

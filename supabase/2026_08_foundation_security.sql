-- Foundation hardening: roles, tenant isolation, and idempotent sync writes.
-- Apply after the existing auth/user profile migrations.

create type if not exists public.app_role as enum (
  'OWNER', 'ADMIN', 'PHARMACIST', 'EMPLOYEE', 'REPRESENTATIVE', 'VIEWER'
);

alter table public.user_profiles
  add column if not exists role_v2 public.app_role;

update public.user_profiles
set role_v2 = case
  when upper(role) = 'ADMIN' then 'ADMIN'::public.app_role
  when upper(role) = 'OWNER' then 'OWNER'::public.app_role
  when upper(role) = 'EMPLOYEE' then 'EMPLOYEE'::public.app_role
  when upper(role) = 'REPRESENTATIVE' then 'REPRESENTATIVE'::public.app_role
  when upper(role) = 'VIEWER' then 'VIEWER'::public.app_role
  else 'PHARMACIST'::public.app_role
end
where role_v2 is null;

alter table public.user_profiles
  alter column role_v2 set default 'VIEWER'::public.app_role,
  alter column role_v2 set not null;

create index if not exists idx_user_profiles_tenant_role_active
  on public.user_profiles(tenant_id, role_v2, active);

create or replace function public.current_app_role_v2()
returns public.app_role
language sql
stable
security definer
set search_path = public
as $$
  select role_v2
  from public.user_profiles
  where user_id = auth.uid()
    and active = true
  limit 1;
$$;

revoke all on function public.current_app_role_v2() from public;
grant execute on function public.current_app_role_v2() to authenticated;

create or replace function public.can_manage_users_v2()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.current_app_role_v2() in ('OWNER'::public.app_role, 'ADMIN'::public.app_role);
$$;

create or replace function public.can_write_catalog_v2()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.current_app_role_v2() in (
    'OWNER'::public.app_role,
    'ADMIN'::public.app_role,
    'PHARMACIST'::public.app_role
  );
$$;

create table if not exists public.sync_operations (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  idempotency_key text not null,
  actor_user_id uuid not null references auth.users(id) on delete restrict,
  operation text not null check (operation in ('UPSERT', 'SOFT_DELETE')),
  entity_type text not null check (entity_type in ('COMPANY', 'REPRESENTATIVE', 'VISIT', 'USER')),
  entity_id text not null,
  payload jsonb not null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  unique (tenant_id, idempotency_key)
);

create index if not exists idx_sync_operations_tenant_created
  on public.sync_operations(tenant_id, created_at);

alter table public.sync_operations enable row level security;

drop policy if exists sync_operations_select_own_tenant on public.sync_operations;
create policy sync_operations_select_own_tenant
on public.sync_operations for select
to authenticated
using (tenant_id = public.current_tenant_id());

drop policy if exists sync_operations_insert_authorized on public.sync_operations;
create policy sync_operations_insert_authorized
on public.sync_operations for insert
to authenticated
with check (
  tenant_id = public.current_tenant_id()
  and actor_user_id = auth.uid()
  and public.can_write_catalog_v2()
);

revoke all on table public.sync_operations from anon;
grant select, insert on table public.sync_operations to authenticated;

-- Legacy shared-token RPCs are intentionally disabled. Writes must use secure-sync.
revoke execute on function public.borg_sync_companies(text, jsonb) from anon, authenticated;
revoke execute on function public.borg_sync_representatives(text, jsonb) from anon, authenticated;
revoke execute on function public.borg_sync_visits(text, jsonb) from anon, authenticated;
revoke execute on function public.borg_sync_users(text, jsonb) from anon, authenticated;
revoke execute on function public.borg_pull_users(text) from anon, authenticated;
revoke execute on function public.borg_login_user(text, text, text) from anon, authenticated;
revoke execute on function public.borg_prune_tenant_to_companies(text, uuid, jsonb) from anon, authenticated;
revoke execute on function public.borg_repair_representative_company_links(text, uuid) from anon, authenticated;
revoke execute on function public.borg_move_representative(text, uuid, uuid) from anon, authenticated;
revoke execute on function public.borg_delete_representative_forever(text, uuid) from anon, authenticated;
revoke execute on function public.borg_delete_company_forever(text, uuid) from anon, authenticated;

comment on table public.sync_operations is 'All Android synchronization writes must be authenticated, tenant-scoped, role-checked, and idempotent through secure-sync.';

-- Rotate/revoke any previously deployed shared token in Supabase secrets before applying this migration.

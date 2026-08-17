-- Phase 2.7.1: deny legacy broad policies and enforce authenticated tenant scope.
begin;

-- Remove every legacy policy that was previously broad or anonymous.
do $$
declare p record;
begin
  for p in select schemaname, tablename, policyname from pg_policies
           where schemaname = 'public'
             and (qual ilike '%true%' or with_check ilike '%true%') loop
    execute format('drop policy if exists %I on %I.%I', p.policyname, p.schemaname, p.tablename);
  end loop;
end $$;

-- Business tables must be visible only to an authenticated user in the same tenant.
-- The tenant is resolved from auth.uid() -> user_profiles, never from client payload.

do $$
declare t text;
begin
  foreach t in array array['companies','representatives','visits'] loop
    if to_regclass('public.' || t) is not null then
      execute format('alter table public.%I enable row level security', t);
      execute format('drop policy if exists %I on public.%I', t || '_select_tenant_secure', t);
      execute format('create policy %I on public.%I for select to authenticated using (tenant_id = public.current_tenant_id() and is_deleted = false)', t || '_select_tenant_secure', t);
      execute format('drop policy if exists %I on public.%I', t || '_admin_insert_tenant_secure', t);
      execute format('create policy %I on public.%I for insert to authenticated with check (tenant_id = public.current_tenant_id() and public.current_app_role() = ''ADMIN'')', t || '_admin_insert_tenant_secure', t);
      execute format('drop policy if exists %I on public.%I', t || '_admin_update_tenant_secure', t);
      execute format('create policy %I on public.%I for update to authenticated using (tenant_id = public.current_tenant_id() and public.current_app_role() = ''ADMIN'') with check (tenant_id = public.current_tenant_id() and public.current_app_role() = ''ADMIN'')', t || '_admin_update_tenant_secure', t);
    end if;
  end loop;
end $$;

create table if not exists public.admin_rate_limits (
  actor_user_id uuid primary key references auth.users(id) on delete cascade,
  window_started_at timestamptz not null default now(),
  request_count integer not null default 0
);
alter table public.admin_rate_limits enable row level security;
revoke all on public.admin_rate_limits from anon, authenticated;
create or replace function public.check_admin_rate_limit(p_actor_user_id uuid, p_limit integer default 10)
returns boolean language plpgsql security definer set search_path=public
as $$
declare v_count integer; v_start timestamptz;
begin
  insert into public.admin_rate_limits(actor_user_id,window_started_at,request_count)
  values(p_actor_user_id,now(),1)
  on conflict(actor_user_id) do update set
    window_started_at=case when now()-admin_rate_limits.window_started_at >= interval '1 minute' then now() else admin_rate_limits.window_started_at end,
    request_count=case when now()-admin_rate_limits.window_started_at >= interval '1 minute' then 1 else admin_rate_limits.request_count+1 end;
  select request_count,window_started_at into v_count,v_start from public.admin_rate_limits where actor_user_id=p_actor_user_id;
  return v_count <= greatest(1,least(p_limit,100));
end;
$$;
revoke all on function public.check_admin_rate_limit(uuid,integer) from public;
grant execute on function public.check_admin_rate_limit(uuid,integer) to service_role;

create table if not exists public.admin_audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_user_id uuid not null references auth.users(id) on delete restrict,
  target_user_id uuid references auth.users(id) on delete set null,
  action text not null,
  detail text not null default '',
  created_at timestamptz not null default now()
);
alter table public.admin_audit_logs enable row level security;
drop policy if exists admin_audit_logs_deny_client on public.admin_audit_logs;
create policy admin_audit_logs_deny_client on public.admin_audit_logs for all to anon, authenticated using (false) with check (false);
revoke all on public.admin_audit_logs from anon, authenticated;

-- No client role can access bot data until bot operations are moved behind authenticated RPCs.
-- This is safer than exposing cross-tenant bot logs through the anon key.

do $$
begin
  if to_regclass('public.bot_config') is not null then
    alter table public.bot_config enable row level security;
    drop policy if exists bot_config_deny_client on public.bot_config;
    create policy bot_config_deny_client on public.bot_config for all to anon, authenticated using (false) with check (false);
  end if;
  if to_regclass('public.bot_logs') is not null then
    alter table public.bot_logs enable row level security;
    drop policy if exists bot_logs_deny_client on public.bot_logs;
    create policy bot_logs_deny_client on public.bot_logs for all to anon, authenticated using (false) with check (false);
  end if;
end $$;

-- Do not derive tenant identity from client-controlled raw_user_meta_data.
-- Profile creation is performed by the authenticated admin Edge Function, which derives
-- tenant_id from the caller's user_profiles row. Direct Auth signups get no tenant profile.
create or replace function public.handle_new_auth_user_profile()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  return new;
end;
$$;

commit;

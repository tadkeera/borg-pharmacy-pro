-- Phase 2 production sync: apply validated operations atomically with audit history.

alter table public.companies add column if not exists sync_version bigint not null default 0;
alter table public.representatives add column if not exists sync_version bigint not null default 0;
alter table public.visits add column if not exists sync_version bigint not null default 0;

create index if not exists idx_companies_tenant_sync_version
  on public.companies(tenant_id, sync_version);
create index if not exists idx_representatives_tenant_sync_version
  on public.representatives(tenant_id, sync_version);
create index if not exists idx_visits_tenant_sync_version
  on public.visits(tenant_id, sync_version);

alter table public.sync_operations
  add column if not exists processed_at timestamptz,
  add column if not exists outcome text check (outcome in ('APPLIED', 'CONFLICT', 'REJECTED')),
  add column if not exists conflict_reason text;

create table if not exists public.sync_audit (
  id uuid primary key default gen_random_uuid(),
  operation_id uuid not null references public.sync_operations(id) on delete cascade,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  actor_user_id uuid not null references auth.users(id) on delete restrict,
  entity_type text not null,
  entity_id text not null,
  outcome text not null check (outcome in ('APPLIED', 'CONFLICT', 'REJECTED')),
  incoming_version bigint not null,
  current_version bigint,
  payload jsonb not null,
  reason text,
  created_at timestamptz not null default now()
);

create index if not exists idx_sync_audit_tenant_created
  on public.sync_audit(tenant_id, created_at desc);
create index if not exists idx_sync_audit_entity
  on public.sync_audit(tenant_id, entity_type, entity_id, created_at desc);

alter table public.sync_audit enable row level security;
drop policy if exists sync_audit_select_own_tenant on public.sync_audit;
create policy sync_audit_select_own_tenant
on public.sync_audit for select
to authenticated
using (tenant_id = public.current_tenant_id());
revoke all on public.sync_audit from anon, authenticated;
grant select on public.sync_audit to authenticated;

create or replace function public.apply_sync_operations(
  p_operation_ids uuid[],
  p_tenant_id uuid,
  p_actor_user_id uuid
)
returns table(
  operation_id uuid,
  entity_type text,
  entity_id text,
  outcome text,
  incoming_version bigint,
  current_version bigint,
  reason text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  op record;
  profile_role public.app_role;
  row_tenant uuid;
  row_version bigint;
  applied_version bigint;
  row_outcome text;
  row_reason text;
  payload jsonb;
  company_id uuid;
  representative_id uuid;
  visit_id uuid;
  incoming_id uuid;
begin
  select role_v2 into profile_role
  from public.user_profiles
  where user_id = p_actor_user_id
    and tenant_id = p_tenant_id
    and active = true;

  if profile_role is null or profile_role not in ('OWNER'::public.app_role, 'ADMIN'::public.app_role, 'PHARMACIST'::public.app_role) then
    raise exception 'sync actor is not authorized for tenant' using errcode = '42501';
  end if;

  for op in
    select * from public.sync_operations
    where id = any(p_operation_ids)
      and tenant_id = p_tenant_id
    order by created_at, id
    for update
  loop
    row_outcome := 'REJECTED';
    row_reason := null;
    row_version := null;
    applied_version := op.version;
    payload := op.payload;

    if op.processed_at is not null then
      operation_id := op.id;
      entity_type := op.entity_type;
      entity_id := op.entity_id;
      outcome := coalesce(op.outcome, 'REJECTED');
      incoming_version := op.version;
      current_version := null;
      reason := op.conflict_reason;
      return next;
      continue;
    elsif op.operation not in ('UPSERT', 'SOFT_DELETE') then
      row_reason := 'unsupported operation';
    elsif op.entity_type = 'COMPANY' then
      company_id := op.entity_id::uuid;
      select tenant_id, sync_version into row_tenant, row_version
      from public.companies where id = company_id for update;
      if row_tenant is not null and row_tenant <> p_tenant_id then
        row_reason := 'cross-tenant entity reference';
      elsif row_version is not null and row_version >= op.version then
        row_outcome := 'CONFLICT';
        row_reason := 'incoming version is not newer';
      else
        insert into public.companies(id, tenant_id, name, tier, base_day_index, base_shift, created_at, updated_at, deleted_at, sync_status, is_deleted, sync_version)
        values (
          company_id, p_tenant_id, coalesce(payload->>'name', ''), coalesce(payload->>'tier', 'UNRATED'),
          nullif(payload->>'base_day_index', '')::int, nullif(payload->>'base_shift', ''),
          coalesce(nullif(payload->>'created_at', '')::bigint, op.version), op.version,
          nullif(payload->>'deleted_at', '')::bigint, 'SYNCED', op.operation = 'SOFT_DELETE' or coalesce((payload->>'is_deleted')::boolean, false), op.version
        )
        on conflict (id) do update set
          name = excluded.name, tier = excluded.tier, base_day_index = excluded.base_day_index,
          base_shift = excluded.base_shift, updated_at = excluded.updated_at, deleted_at = excluded.deleted_at,
          sync_status = 'SYNCED', is_deleted = excluded.is_deleted, sync_version = excluded.sync_version;
        row_outcome := 'APPLIED';
      end if;
    elsif op.entity_type = 'REPRESENTATIVE' then
      representative_id := op.entity_id::uuid;
      select tenant_id, sync_version into row_tenant, row_version
      from public.representatives where id = representative_id for update;
      if row_tenant is not null and row_tenant <> p_tenant_id then
        row_reason := 'cross-tenant entity reference';
      elsif row_version is not null and row_version >= op.version then
        row_outcome := 'CONFLICT';
        row_reason := 'incoming version is not newer';
      elsif not exists (select 1 from public.companies where id = (payload->>'company_id')::uuid and tenant_id = p_tenant_id and is_deleted = false) then
        row_reason := 'company does not belong to tenant';
      else
        insert into public.representatives(id, tenant_id, company_id, name, phone, created_at, updated_at, deleted_at, sync_status, is_deleted, sync_version)
        values (
          representative_id, p_tenant_id, (payload->>'company_id')::uuid, coalesce(payload->>'name', ''), coalesce(payload->>'phone', ''),
          coalesce(nullif(payload->>'created_at', '')::bigint, op.version), op.version,
          nullif(payload->>'deleted_at', '')::bigint, 'SYNCED', op.operation = 'SOFT_DELETE' or coalesce((payload->>'is_deleted')::boolean, false), op.version
        )
        on conflict (id) do update set
          company_id = excluded.company_id, name = excluded.name, phone = excluded.phone,
          updated_at = excluded.updated_at, deleted_at = excluded.deleted_at, sync_status = 'SYNCED',
          is_deleted = excluded.is_deleted, sync_version = excluded.sync_version;
        row_outcome := 'APPLIED';
      end if;
    elsif op.entity_type = 'VISIT' then
      visit_id := op.entity_id::uuid;
      select tenant_id, sync_version into row_tenant, row_version
      from public.visits where id = visit_id for update;
      if row_tenant is not null and row_tenant <> p_tenant_id then
        row_reason := 'cross-tenant entity reference';
      elsif row_version is not null and row_version >= op.version then
        row_outcome := 'CONFLICT';
        row_reason := 'incoming version is not newer';
      elsif not exists (select 1 from public.companies where id = (payload->>'company_id')::uuid and tenant_id = p_tenant_id and is_deleted = false) then
        row_reason := 'company does not belong to tenant';
      else
        insert into public.visits(id, tenant_id, company_id, cycle_start_epoch_day, day_of_cycle, week_of_cycle, date_epoch_day, shift, slot_index, status, created_at, updated_at, deleted_at, sync_status, is_deleted, sync_version)
        values (
          visit_id, p_tenant_id, (payload->>'company_id')::uuid,
          (payload->>'cycle_start_epoch_day')::bigint,
          greatest(1, least(28, coalesce((payload->>'day_of_cycle')::int, 1))),
          greatest(1, least(4, coalesce((payload->>'week_of_cycle')::int, 1))),
          (payload->>'date_epoch_day')::bigint, coalesce(payload->>'shift', 'MORNING'),
          coalesce((payload->>'slot_index')::int, 0), coalesce(payload->>'status', 'SCHEDULED'),
          coalesce(nullif(payload->>'created_at', '')::bigint, op.version), op.version,
          nullif(payload->>'deleted_at', '')::bigint, 'SYNCED', op.operation = 'SOFT_DELETE' or coalesce((payload->>'is_deleted')::boolean, false), op.version
        )
        on conflict (id) do update set
          company_id = excluded.company_id, cycle_start_epoch_day = excluded.cycle_start_epoch_day,
          day_of_cycle = excluded.day_of_cycle, week_of_cycle = excluded.week_of_cycle,
          date_epoch_day = excluded.date_epoch_day, shift = excluded.shift, slot_index = excluded.slot_index,
          status = excluded.status, updated_at = excluded.updated_at, deleted_at = excluded.deleted_at,
          sync_status = 'SYNCED', is_deleted = excluded.is_deleted, sync_version = excluded.sync_version;
        row_outcome := 'APPLIED';
      end if;
    else
      row_reason := 'entity type is not enabled for mobile sync';
    end if;

    insert into public.sync_audit(operation_id, tenant_id, actor_user_id, entity_type, entity_id, outcome, incoming_version, current_version, payload, reason)
    values (op.id, p_tenant_id, p_actor_user_id, op.entity_type, op.entity_id, row_outcome, op.version, row_version, payload, row_reason);

    update public.sync_operations
    set processed_at = now(), outcome = row_outcome, conflict_reason = row_reason
    where id = op.id;

    operation_id := op.id;
    entity_type := op.entity_type;
    entity_id := op.entity_id;
    outcome := row_outcome;
    incoming_version := op.version;
    current_version := row_version;
    reason := row_reason;
    return next;
  end loop;
end;
$$;

revoke all on function public.apply_sync_operations(uuid[], uuid, uuid) from public, anon, authenticated;
grant execute on function public.apply_sync_operations(uuid[], uuid, uuid) to service_role;

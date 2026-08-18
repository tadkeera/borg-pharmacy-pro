-- Phase 2.7.2: authenticated, tenant-scoped push/pull synchronization.
begin;
create extension if not exists pgcrypto;

create table if not exists public.sync_operations (
  operation_id uuid primary key,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  idempotency_key text not null,
  entity_type text not null check (entity_type in ('COMPANY','REPRESENTATIVE','VISIT')),
  entity_id uuid not null,
  operation text not null check (operation in ('CREATE','UPDATE','DELETE')),
  local_version bigint not null,
  payload jsonb not null,
  accepted_at timestamptz not null default now(),
  unique(tenant_id,idempotency_key)
);
create table if not exists public.sync_changes (
  sequence_id bigint generated always as identity primary key,
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  entity_type text not null,
  entity_id uuid not null,
  server_version bigint not null,
  payload jsonb not null,
  deleted boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists idx_sync_changes_tenant_sequence on public.sync_changes(tenant_id,sequence_id);
alter table public.sync_operations enable row level security;
alter table public.sync_changes enable row level security;
drop policy if exists sync_operations_tenant on public.sync_operations;
create policy sync_operations_tenant on public.sync_operations for select to authenticated using (tenant_id=public.current_tenant_id());
drop policy if exists sync_changes_tenant on public.sync_changes;
create policy sync_changes_tenant on public.sync_changes for select to authenticated using (tenant_id=public.current_tenant_id());

create or replace function public.sync_push(p_operation jsonb)
returns jsonb language plpgsql security definer set search_path=public
as $$
declare t uuid:=public.current_tenant_id(); op uuid:=(p_operation->>'operation_id')::uuid; idem text:=p_operation->>'idempotency_key'; et text:=p_operation->>'entity_type'; eid uuid:=(p_operation->>'entity_id')::uuid; payload jsonb:=coalesce(p_operation->'payload','{}'::jsonb); local_v bigint:=coalesce((p_operation->>'local_version')::bigint,0); existing public.sync_operations%rowtype; server_v bigint;
begin
 if t is null then raise exception 'tenant not resolved' using errcode='42501'; end if;
 select * into existing from public.sync_operations where tenant_id=t and idempotency_key=idem limit 1;
 if found then return jsonb_build_object('accepted',true,'duplicate',true,'server_version',existing.local_version); end if;
 if et='COMPANY' then select updated_at into server_v from public.companies where id=eid and tenant_id=t;
 elsif et='REPRESENTATIVE' then select updated_at into server_v from public.representatives where id=eid and tenant_id=t;
 elsif et='VISIT' then select updated_at into server_v from public.visits where id=eid and tenant_id=t;
 else raise exception 'unsupported entity type'; end if;
 if server_v is not null and server_v > local_v then
   insert into public.sync_operations(operation_id,tenant_id,idempotency_key,entity_type,entity_id,operation,local_version,payload) values(op,t,idem,et,eid,p_operation->>'operation',local_v,payload) on conflict do nothing;
   return jsonb_build_object('accepted',false,'conflict',true,'server_version',server_v);
 end if;
 insert into public.sync_operations(operation_id,tenant_id,idempotency_key,entity_type,entity_id,operation,local_version,payload) values(op,t,idem,et,eid,p_operation->>'operation',local_v,payload);
 if et='COMPANY' then
   if p_operation->>'operation'='DELETE' then update companies set deleted_at=extract(epoch from now())*1000,is_deleted=true,updated_at=extract(epoch from now())*1000 where id=eid and tenant_id=t;
   else insert into companies(id,tenant_id,name,tier,base_day_index,base_shift,created_at,updated_at,deleted_at,is_deleted) values(eid,t,payload->>'name',coalesce(payload->>'tier','UNRATED'),nullif(payload->>'base_day_index','')::int,payload->>'base_shift',coalesce((payload->>'created_at')::bigint,extract(epoch from now())*1000),extract(epoch from now())*1000,nullif(payload->>'deleted_at','')::bigint,false) on conflict(id) do update set name=excluded.name,tier=excluded.tier,base_day_index=excluded.base_day_index,base_shift=excluded.base_shift,updated_at=excluded.updated_at,deleted_at=excluded.deleted_at,is_deleted=excluded.is_deleted where companies.tenant_id=t; end if;
 elsif et='REPRESENTATIVE' then
   if p_operation->>'operation'='DELETE' then update representatives set deleted_at=extract(epoch from now())*1000,updated_at=extract(epoch from now())*1000 where id=eid and tenant_id=t;
   else insert into representatives(id,tenant_id,company_id,name,phone,created_at,updated_at,deleted_at) values(eid,t,(payload->>'company_id')::uuid,payload->>'name',coalesce(payload->>'phone',''),coalesce((payload->>'created_at')::bigint,extract(epoch from now())*1000),extract(epoch from now())*1000,null) on conflict(id) do update set company_id=excluded.company_id,name=excluded.name,phone=excluded.phone,updated_at=excluded.updated_at where representatives.tenant_id=t; end if;
 elsif et='VISIT' then
   if p_operation->>'operation'='DELETE' then update visits set deleted_at=extract(epoch from now())*1000,updated_at=extract(epoch from now())*1000 where id=eid and tenant_id=t;
   else insert into visits(id,tenant_id,company_id,cycle_start_epoch_day,day_of_cycle,week_of_cycle,date_epoch_day,shift,slot_index,status,created_at,updated_at,deleted_at) values(eid,t,(payload->>'company_id')::uuid,(payload->>'cycle_start_epoch_day')::bigint,(payload->>'day_of_cycle')::int,(payload->>'week_of_cycle')::int,(payload->>'date_epoch_day')::bigint,payload->>'shift',(payload->>'slot_index')::int,coalesce(payload->>'status','SCHEDULED'),coalesce((payload->>'created_at')::bigint,extract(epoch from now())*1000),extract(epoch from now())*1000,null) on conflict(id) do update set status=excluded.status,shift=excluded.shift,date_epoch_day=excluded.date_epoch_day,updated_at=excluded.updated_at where visits.tenant_id=t; end if;
 end if;
 return jsonb_build_object('accepted',true,'duplicate',false,'server_version',extract(epoch from now())::bigint);
end;
$$;

create or replace function public.sync_pull(p_cursor bigint default 0,p_page_size int default 100)
returns table(sequence_id bigint,entity_type text,entity_id uuid,server_version bigint,payload jsonb,deleted boolean)
language sql security invoker set search_path=public
as $$ select sequence_id,entity_type,entity_id,server_version,payload,deleted from public.sync_changes where tenant_id=public.current_tenant_id() and sequence_id>coalesce(p_cursor,0) order by sequence_id limit least(greatest(coalesce(p_page_size,100),1),500) $$;
grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(bigint,int) to authenticated;
create or replace function public.capture_sync_change() returns trigger language plpgsql security definer set search_path=public as $$
declare et text; eid uuid; t uuid; v bigint; body jsonb; gone boolean;
begin
 et:=case when tg_table_name='companies' then 'COMPANY' when tg_table_name='representatives' then 'REPRESENTATIVE' else 'VISIT' end;
 eid:=coalesce(new.id,old.id); t:=coalesce(new.tenant_id,old.tenant_id); v:=coalesce(new.updated_at,extract(epoch from now())*1000); body:=to_jsonb(coalesce(new,old)); gone:=coalesce(new.is_deleted,false) or coalesce(new.deleted_at is not null,false);
 if t is not null then insert into public.sync_changes(tenant_id,entity_type,entity_id,server_version,payload,deleted) values(t,et,eid,v,body,gone); end if;
 return new;
end; $$;
drop trigger if exists companies_sync_change on public.companies; create trigger companies_sync_change after insert or update on public.companies for each row execute function public.capture_sync_change();
drop trigger if exists representatives_sync_change on public.representatives; create trigger representatives_sync_change after insert or update on public.representatives for each row execute function public.capture_sync_change();
drop trigger if exists visits_sync_change on public.visits; create trigger visits_sync_change after insert or update on public.visits for each row execute function public.capture_sync_change();
commit;

-- Phase 2.7.1 RLS regression test. Requires CI-created user_a_id and user_b_id psql variables.
\if :{?user_a_id}
\else
  \echo 'user_a_id is required';
  \quit 2
\endif
\if :{?user_b_id}
\else
  \echo 'user_b_id is required';
  \quit 2
\endif
begin;
set local role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', true);
select set_config('request.jwt.claim.sub', :'user_a_id', true);
do $$
declare own_count integer; foreign_count integer;
begin
  select count(*) into own_count from public.companies where tenant_id = public.current_tenant_id();
  if own_count <> 1 then raise exception 'Tenant A own read failed: expected 1, got %', own_count; end if;
  select count(*) into foreign_count from public.companies where tenant_id = '00000000-0000-0000-0000-00000000000b'::uuid;
  if foreign_count <> 0 then raise exception 'Tenant A cross-tenant read was allowed'; end if;
  begin
    insert into public.companies(id,tenant_id,name,tier,created_at,updated_at)
    values(gen_random_uuid(),'00000000-0000-0000-0000-00000000000b','cross-tenant-write','UNRATED',0,0);
    raise exception 'Tenant A cross-tenant write was allowed';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claim.sub', :'user_b_id', true);
do $$
declare own_count integer; foreign_count integer;
begin
  select count(*) into own_count from public.companies where tenant_id = public.current_tenant_id();
  if own_count <> 1 then raise exception 'Tenant B own read failed: expected 1, got %', own_count; end if;
  select count(*) into foreign_count from public.companies where tenant_id = '00000000-0000-0000-0000-00000000000a'::uuid;
  if foreign_count <> 0 then raise exception 'Tenant B cross-tenant read was allowed'; end if;
  begin
    insert into public.companies(id,tenant_id,name,tier,created_at,updated_at)
    values(gen_random_uuid(),'00000000-0000-0000-0000-00000000000a','cross-tenant-write','UNRATED',0,0);
    raise exception 'Tenant B cross-tenant write was allowed';
  exception when insufficient_privilege then null;
  end;
end $$;
rollback;
\echo 'Tenant A and Tenant B RLS isolation checks passed.'

-- Phase 2.7.1 RLS regression test (pgTAP-style SQL).
-- The CI workflow creates the two authenticated fixture users and supplies their IDs.
\if :{?user_a_id}
\else
  \echo 'user_a_id is required';
  \quit 2
\endif
begin;
select plan(4);
select set_config('request.jwt.claim.sub', :'user_a_id', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
set local role authenticated;
select is((select count(*)::int from public.companies where tenant_id = public.current_tenant_id()), 1, 'tenant A sees its own fixture row');
select is((select count(*)::int from public.representatives where tenant_id <> public.current_tenant_id()), 0, 'tenant A cannot see representatives from another tenant');
select is((select count(*)::int from public.visits where tenant_id <> public.current_tenant_id()), 0, 'tenant A cannot see visits from another tenant');
select throws_ok($$insert into public.companies(id,tenant_id,name,created_at,updated_at) values(gen_random_uuid(),'00000000-0000-0000-0000-00000000000b','cross-tenant',0,0)$$, '42501', null, 'tenant A cannot insert into tenant B');
select * from finish();
rollback;

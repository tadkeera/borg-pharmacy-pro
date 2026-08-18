-- Phase 2.7.2 live contract test. CI supplies user_a_id and user_b_id.
\if :{?user_a_id}
\else \echo 'user_a_id required'; \quit 2
\endif
\if :{?user_b_id}
\else \echo 'user_b_id required'; \quit 2
\endif
begin;
set local role authenticated;
select set_config('request.jwt.claim.role','authenticated',true);
select set_config('request.jwt.claim.sub',:'user_a_id',true);
select public.sync_push(jsonb_build_object('operation_id','20000000-0000-0000-0000-00000000000a','idempotency_key','phase271-sync-a-company','entity_type','COMPANY','entity_id','10000000-0000-0000-0000-00000000000a','operation','CREATE','local_version',1,'payload',jsonb_build_object('name','Sync Company A','tier','UNRATED')));
select public.sync_push(jsonb_build_object('operation_id','20000000-0000-0000-0000-00000000000a','idempotency_key','phase271-sync-a-company','entity_type','COMPANY','entity_id','10000000-0000-0000-0000-00000000000a','operation','CREATE','local_version',1,'payload',jsonb_build_object('name','Sync Company A','tier','UNRATED')));
select public.sync_push(jsonb_build_object('operation_id','20000000-0000-0000-0000-00000000000b','idempotency_key','phase271-sync-a-rep','entity_type','REPRESENTATIVE','entity_id','20000000-0000-0000-0000-00000000000b','operation','CREATE','local_version',1,'payload',jsonb_build_object('company_id','10000000-0000-0000-0000-00000000000a','name','Rep A','phone','+967')));
select public.sync_push(jsonb_build_object('operation_id','20000000-0000-0000-0000-00000000000c','idempotency_key','phase271-sync-a-visit','entity_type','VISIT','entity_id','30000000-0000-0000-0000-00000000000a','operation','CREATE','local_version',1,'payload',jsonb_build_object('company_id','10000000-0000-0000-0000-00000000000a','cycle_start_epoch_day',1,'day_of_cycle',1,'week_of_cycle',1,'date_epoch_day',1,'shift','MORNING','slot_index',0)));
do $$ declare n int; c bigint; begin
 select count(*) into n from public.sync_pull(0,100); if n < 3 then raise exception 'sync_pull returned fewer than 3 changes'; end if;
 select max(sequence_id) into c from public.sync_pull(0,2); if c is null then raise exception 'cursor page missing sequence'; end if;
 if (select count(*) from public.sync_pull(c,100)) < 1 then raise exception 'cursor did not advance to the next page'; end if;
end $$;
select set_config('request.jwt.claim.sub',:'user_b_id',true);
do $$ begin if (select count(*) from public.sync_pull(0,100)) <> 0 then raise exception 'Tenant B can read Tenant A changes'; end if; end $$;
rollback;
\echo 'sync_push, duplicate replay, sync_pull cursor pagination, and tenant isolation passed.'

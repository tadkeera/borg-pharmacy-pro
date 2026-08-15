-- حماية الشركات المحذوفة من الرجوع للحياة بسبب أجهزة قديمة أو غير مصرح لها بالرفع.
-- هذا الزناد يمنع أي UPDATE أقدم/مساوٍ لتوقيت الحذف من تحويل الشركة المحذوفة إلى نشطة.

begin;

create or replace function public.prevent_company_resurrection()
returns trigger
language plpgsql
as $$
begin
  if old.is_deleted = true or old.deleted_at is not null then
    if (coalesce(new.is_deleted, false) = false or new.deleted_at is null)
       and coalesce(new.updated_at, 0) <= coalesce(old.updated_at, 0) then
      new.is_deleted := true;
      new.deleted_at := old.deleted_at;
      new.updated_at := old.updated_at;
      new.sync_status := coalesce(old.sync_status, new.sync_status, 'SYNCED');
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_prevent_company_resurrection on public.companies;
create trigger trg_prevent_company_resurrection
before update on public.companies
for each row
execute function public.prevent_company_resurrection();

commit;

select json_build_object(
  'trigger_installed', exists (
    select 1
    from pg_trigger t
    join pg_class c on c.oid = t.tgrelid
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relname = 'companies'
      and t.tgname = 'trg_prevent_company_resurrection'
      and not t.tgisinternal
  )
) as prevent_company_resurrection_status;

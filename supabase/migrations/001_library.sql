-- Apply in the Supabase SQL editor or with `supabase db push`.
create table public.library_records (
    owner uuid not null references auth.users(id) on delete cascade default auth.uid(),
    id text not null check (length(id) between 1 and 256),
    kind text not null check (kind in ('track','playlist','like','follow','event','preferences')),
    payload text not null check (octet_length(payload) <= 1048576),
    revision bigint not null default 1,
    deleted boolean not null default false,
    updated_at timestamptz not null default now(),
    primary key(owner, id)
);
create index library_records_owner_updated on public.library_records(owner, updated_at);
alter table public.library_records enable row level security;
create policy "Read own records" on public.library_records for select to authenticated using (owner = auth.uid());
-- Mutations must pass through the revision-checked RPC. No direct client table writes.
revoke all on public.library_records from anon, authenticated;
grant select on public.library_records to authenticated;

create or replace function public.apply_library_record(record_id text, record_kind text, record_payload text, base_revision bigint, is_deleted boolean)
returns boolean language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
    if auth.uid() is null then raise exception 'Not authenticated'; end if;
    if base_revision = 0 then
        insert into public.library_records(owner,id,kind,payload,deleted)
        values(auth.uid(),record_id,record_kind,record_payload,is_deleted)
        on conflict do nothing;
    else
        update public.library_records set payload=record_payload, kind=record_kind,
          deleted=is_deleted, revision=revision+1, updated_at=now()
        where owner=auth.uid() and id=record_id and revision=base_revision;
    end if;
    get diagnostics affected = row_count;
    return affected = 1;
end $$;
revoke all on function public.apply_library_record(text,text,text,bigint,boolean) from public, anon;
grant execute on function public.apply_library_record(text,text,text,bigint,boolean) to authenticated;

create or replace function public.delete_own_account() returns void
language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null then raise exception 'Not authenticated'; end if;
    delete from auth.users where id=auth.uid();
end $$;
revoke all on function public.delete_own_account() from public, anon;
grant execute on function public.delete_own_account() to authenticated;

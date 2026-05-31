alter table user_accounts add column status text;

update user_accounts
set status = case
    when lower(email) = 'de1m0s242@gmail.com' then 'active'
    else 'pending'
end;

alter table user_accounts alter column status set not null;

alter table user_accounts
    add constraint user_accounts_status_check check (status in ('pending', 'active', 'deactivated'));

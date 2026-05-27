create table user_accounts (
    id uuid primary key,
    provider text not null,
    provider_subject text not null,
    email text not null,
    display_name text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_login_at timestamptz not null,
    constraint user_accounts_provider_subject_unique unique (provider, provider_subject),
    constraint user_accounts_email_unique unique (email)
);

create index user_accounts_provider_subject_idx on user_accounts (provider, provider_subject);

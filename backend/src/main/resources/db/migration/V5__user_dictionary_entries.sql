create table user_dictionary_entries (
    id uuid primary key,
    user_account_id uuid not null,
    word_info_record_id uuid not null,
    normalized_word text not null,
    due_at timestamptz not null,
    last_reviewed_at timestamptz,
    last_result text,
    repetition_count integer not null,
    correct_streak integer not null,
    lapse_count integer not null,
    interval_days integer not null,
    ease_factor double precision not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint user_dictionary_entries_user_word_unique unique (user_account_id, normalized_word),
    constraint user_dictionary_entries_user_account_fk
        foreign key (user_account_id) references user_accounts (id),
    constraint user_dictionary_entries_word_info_record_fk
        foreign key (word_info_record_id) references word_info_records (id)
);

create index user_dictionary_entries_user_due_at_idx on user_dictionary_entries (user_account_id, due_at);
create index user_dictionary_entries_word_info_record_idx on user_dictionary_entries (word_info_record_id);

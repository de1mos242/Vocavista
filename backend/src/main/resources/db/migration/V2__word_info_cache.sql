create table word_info_records (
    id uuid primary key,
    normalized_query text not null,
    normalized_word text not null,
    language text not null,
    response_json text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint word_info_records_normalized_query_unique unique (normalized_query)
);

create index word_info_records_normalized_word_idx on word_info_records (normalized_word);

alter table pronunciation_assets
    add column word_info_record_id uuid not null;

alter table pronunciation_assets
    add constraint pronunciation_assets_word_info_record_fk
        foreign key (word_info_record_id) references word_info_records (id);

create index pronunciation_assets_word_info_record_idx on pronunciation_assets (word_info_record_id);

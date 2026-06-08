create table phrase_image_assets (
    id uuid primary key,
    word_info_record_id uuid not null,
    normalized_word text not null,
    normalized_phrase text not null,
    input_word text not null,
    input_phrase text not null,
    language text not null,
    status text not null,
    image_object_key text,
    image_provider text,
    image_model text,
    prompt_version text not null,
    prompt_text text,
    content_hash text not null,
    error_code text,
    error_message text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    rejected_at timestamptz,
    constraint phrase_image_assets_word_info_record_fk
        foreign key (word_info_record_id) references word_info_records (id)
);

create unique index phrase_image_assets_active_cache_unique
    on phrase_image_assets (language, content_hash)
    where status <> 'REJECTED';

create index phrase_image_assets_word_info_record_status_idx
    on phrase_image_assets (word_info_record_id, status, updated_at desc);

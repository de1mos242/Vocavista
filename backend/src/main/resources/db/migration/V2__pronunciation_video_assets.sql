create table pronunciation_video_assets (
    id uuid primary key,
    normalized_word text not null,
    normalized_phrase text not null,
    input_word text not null,
    input_phrase text not null,
    language text not null,
    status text not null,
    audio_object_key text,
    video_object_key text,
    audio_provider text,
    audio_model text,
    video_provider text,
    video_model text,
    content_hash text not null,
    error_code text,
    error_message text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    constraint pronunciation_video_assets_language_hash_unique unique (language, content_hash)
);

create index pronunciation_video_assets_status_idx on pronunciation_video_assets (status);

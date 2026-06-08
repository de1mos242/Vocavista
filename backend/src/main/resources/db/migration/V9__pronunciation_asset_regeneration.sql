alter table pronunciation_assets
    add column rejected_at timestamptz;

alter table pronunciation_assets
    drop constraint pronunciation_assets_language_hash_unique;

create unique index pronunciation_assets_active_cache_unique
    on pronunciation_assets (language, content_hash)
    where status <> 'REJECTED';

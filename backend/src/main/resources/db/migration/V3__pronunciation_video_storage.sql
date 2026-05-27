alter table pronunciation_assets
    drop column audio_object_key;

alter table pronunciation_assets
    drop column audio_provider;

alter table pronunciation_assets
    drop column audio_model;

alter table pronunciation_assets
    add column video_object_key text;

alter table pronunciation_assets
    add column video_provider text;

alter table pronunciation_assets
    add column video_model text;

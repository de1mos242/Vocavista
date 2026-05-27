alter table pronunciation_assets
    add column render_mode text not null default 'talking-head';

alter table pronunciation_assets
    add column video_object_key text;

alter table pronunciation_assets
    add column video_provider text;

alter table pronunciation_assets
    add column video_model text;

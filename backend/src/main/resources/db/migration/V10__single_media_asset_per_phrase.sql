delete from pronunciation_assets
where status = 'REJECTED';

delete from phrase_image_assets
where status = 'REJECTED';

with ranked_pronunciations as (
    select id,
           row_number() over (
               partition by word_info_record_id, normalized_phrase
               order by
                   case status
                       when 'COMPLETED' then 0
                       when 'PROCESSING' then 1
                       when 'QUEUED' then 2
                       else 3
                   end,
                   updated_at desc
           ) as row_number
    from pronunciation_assets
)
delete from pronunciation_assets asset
using ranked_pronunciations ranked
where asset.id = ranked.id
  and ranked.row_number > 1;

with ranked_phrase_images as (
    select id,
           row_number() over (
               partition by word_info_record_id, normalized_phrase
               order by
                   case status
                       when 'COMPLETED' then 0
                       when 'PROCESSING' then 1
                       when 'QUEUED' then 2
                       else 3
                   end,
                   updated_at desc
           ) as row_number
    from phrase_image_assets
)
delete from phrase_image_assets asset
using ranked_phrase_images ranked
where asset.id = ranked.id
  and ranked.row_number > 1;

drop index if exists pronunciation_assets_active_cache_unique;
drop index if exists phrase_image_assets_active_cache_unique;

alter table pronunciation_assets
    drop column if exists rejected_at;

alter table phrase_image_assets
    drop column if exists rejected_at;

create unique index pronunciation_assets_word_info_phrase_unique
    on pronunciation_assets (word_info_record_id, normalized_phrase);

create unique index phrase_image_assets_word_info_phrase_unique
    on phrase_image_assets (word_info_record_id, normalized_phrase);

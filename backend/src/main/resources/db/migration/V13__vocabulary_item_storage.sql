delete from phrase_image_assets;
delete from pronunciation_assets;
delete from user_dictionary_entries;
delete from word_info_records;

create table vocabulary_items (
    id uuid primary key,
    language text not null,
    word text not null,
    phrase text not null,
    part_of_speech text not null,
    gender text,
    plural text,
    frequency text not null,
    is_compound boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index vocabulary_items_language_word_idx on vocabulary_items (language, lower(btrim(word)));

create table vocabulary_item_translations (
    id uuid primary key,
    vocabulary_item_id uuid not null,
    language text not null,
    word_translation text not null,
    phrase_translation text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint vocabulary_item_translations_item_fk
        foreign key (vocabulary_item_id) references vocabulary_items (id) on delete cascade,
    constraint vocabulary_item_translations_item_language_unique unique (vocabulary_item_id, language)
);

alter table user_dictionary_entries
    drop constraint user_dictionary_entries_user_word_unique,
    drop constraint user_dictionary_entries_word_info_record_fk,
    drop column normalized_word;

alter table user_dictionary_entries
    rename column word_info_record_id to vocabulary_item_id;

alter table user_dictionary_entries
    add constraint user_dictionary_entries_vocabulary_item_fk
        foreign key (vocabulary_item_id) references vocabulary_items (id),
    add constraint user_dictionary_entries_user_item_unique unique (user_account_id, vocabulary_item_id);

drop index if exists user_dictionary_entries_word_info_record_idx;
create index user_dictionary_entries_vocabulary_item_idx on user_dictionary_entries (vocabulary_item_id);

alter table pronunciation_assets
    drop constraint pronunciation_assets_word_info_record_fk,
    drop column normalized_word,
    drop column normalized_phrase;

alter table pronunciation_assets
    rename column word_info_record_id to vocabulary_item_id;

alter table pronunciation_assets
    add constraint pronunciation_assets_vocabulary_item_fk
        foreign key (vocabulary_item_id) references vocabulary_items (id);

drop index if exists pronunciation_assets_word_info_record_idx;
drop index if exists pronunciation_assets_word_info_phrase_unique;
create index pronunciation_assets_vocabulary_item_idx on pronunciation_assets (vocabulary_item_id);
create unique index pronunciation_assets_vocabulary_item_phrase_unique
    on pronunciation_assets (vocabulary_item_id, lower(btrim(input_phrase)));

alter table phrase_image_assets
    drop constraint phrase_image_assets_word_info_record_fk,
    drop column normalized_word,
    drop column normalized_phrase;

alter table phrase_image_assets
    rename column word_info_record_id to vocabulary_item_id;

alter table phrase_image_assets
    add constraint phrase_image_assets_vocabulary_item_fk
        foreign key (vocabulary_item_id) references vocabulary_items (id);

drop index if exists phrase_image_assets_word_info_record_status_idx;
drop index if exists phrase_image_assets_word_info_phrase_unique;
create index phrase_image_assets_vocabulary_item_status_idx
    on phrase_image_assets (vocabulary_item_id, status, updated_at desc);
create unique index phrase_image_assets_vocabulary_item_phrase_unique
    on phrase_image_assets (vocabulary_item_id, lower(btrim(input_phrase)));

drop table word_info_records;

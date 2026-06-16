create unique index if not exists vocabulary_items_language_word_phrase_unique
    on vocabulary_items (language, lower(btrim(word)), lower(btrim(phrase)));

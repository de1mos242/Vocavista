# App Specification: Vocavista

## Current Product Shape

Vocavista teaches German vocabulary through compact AI-generated word metadata and pronunciation practice.

The current backend supports:

- German word information for learners who know English and Russian.
- Cached word autocomplete across generated word info and pronunciation records.
- Pronunciation media generation for a target word and contextual German phrase.
- Direct Veo video playback for pronunciation media.
- Central media reuse by caching generated pronunciation media metadata in PostgreSQL and storing media bytes in S3-compatible object storage.

## Word Information

`GET /api/v1/words/info?word=Hausaufgabe` returns:

- Normalized German word.
- English and Russian translations.
- Part of speech and frequency.
- German noun fields when relevant: gender, article, and plural.
- Compound-part meanings when relevant.
- Three German example sentences with English and Russian translations.
- Generated word-info responses are stored and reused for the same normalized query to reduce AI provider calls.

## Word Suggestions

`GET /api/v1/words/suggestions?query=haus` searches stored word-info records and generated pronunciation records with simple contains matching.

Suggestions can include:

- Cached word-info words.
- Previously generated word and phrase pairs.
- Existing pronunciation status plus `videoUrl` when completed media can be reused.

## Pronunciation Media

`POST /api/v1/media/pronunciations` accepts a German `word`, `phrase`, and `language` value of `de`.

Generation behavior:

- The word is spoken twice with different pacing.
- For nouns with a known German article, the second repetition includes the article, for example `Zugabe ... die Zugabe ... ich möchte meine Zugabe machen`.
- The contextual phrase is spoken after the repeated word.
- Veo generates a direct vertical lip-sync MP4.
- For nouns with known gender, the Veo speaker follows the word gender: masculine uses a male speaker, feminine uses a female speaker, and neuter uses a young adult woman. Non-nouns also use a young adult woman.
- S3-compatible storage keeps the generated video object.
- The API returns `videoUrl` for same-origin browser playback when default video generation completes.
- The dedicated Veo preview page lets the user search for a word, choose one of the generated example phrases, generate Veo video, and play the returned `videoUrl`.

## Current Technical Shape

- Backend: Java 25 with Spring Boot 4.
- API contract: OpenAPI-generated Spring MVC interfaces and DTOs.
- Database: PostgreSQL with Flyway.
- Storage: S3-compatible media storage, RustFS locally.
- Tests: JUnit, Mockito, and Testcontainers for PostgreSQL-backed context testing.

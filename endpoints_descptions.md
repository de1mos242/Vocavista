# Endpoint behavior descriptions

Source of truth: `backend/src/main/resources/openapi/vocavista-api.yaml` plus the Spring controllers/services that implement it.

Legend:

```text
[HTTP] -> Controller -> Service -> Repository / external API -> Mapper / Response
```

> Cross-cutting: endpoints with `@RequireFunctionalAccess` first pass authenticated Google OAuth and functional-access checks before controller code runs. Most endpoints then load the current `UserAccount` from the Spring Security OIDC principal when they need user-specific data.

## Auth

### getCurrentUser — return the authenticated application user

```text
GET /api/v1/auth/me
  -> AuthController
  -> CurrentUserService
  -> UserAccountRepository
  -> CurrentUserResponse
```

1. Read the authenticated OIDC principal from Spring Security.
2. Load `UserAccount` by provider `GOOGLE` and Google subject.
3. Map account fields to `CurrentUserResponse`, adding admin-list membership and functional-access flags.

## Admin

### listAdminUsers — list all users for admin management

```text
GET /api/v1/admin/users
  -> AdminController
  -> AdminUserManagementService
  -> UserAccountRepository
  -> AdminUsersResponse
```

1. Load current `UserAccount` and verify it is an admin-list user.
2. Load all `UserAccount` entities ordered by creation time.
3. Map each user to `AdminUserResponse`, including whether the row is protected by the admin list.

### updateAdminUserStatus — change a non-admin-list user's status

```text
PUT /api/v1/admin/users/{id}/status
  -> AdminController
  -> AdminUserManagementService
  -> UserAccountRepository
  -> AdminUserResponse
```

1. Load current `UserAccount` and verify it is an admin-list user.
2. Validate that request body contains `status`.
3. Load target `UserAccount` by path `id`.
4. Reject the update if the target user is an admin-list user.
5. Convert API `UserStatus` to entity `UserAccountStatus`, save the target account, and map it to `AdminUserResponse`.

## Dictionary

### addDictionaryEntry — add a generated word-info record to the current user's review list

```text
POST /api/v1/dictionary/entries
  -> DictionaryController
  -> UserDictionaryService
  -> WordInfoRepository + UserDictionaryEntryRepository
  -> AddDictionaryEntryResponse
```

1. Validate that request body contains `wordInfoId`.
2. Load current `UserAccount`.
3. Load `WordInfoRecord` by `wordInfoId`.
4. Find the user's `UserDictionaryEntry` by normalized word; refresh its linked `WordInfoRecord` if it exists.
5. Otherwise create and save a new `UserDictionaryEntry`; on unique-key race, reload and refresh the existing entry.
6. Map the entry identifiers, normalized word, and due date to `AddDictionaryEntryResponse`.

### getDictionaryReview — show words for review

```text
GET /api/v1/dictionary/review?limit=&includeUpcoming=
  -> DictionaryController
  -> UserDictionaryService
  -> UserDictionaryEntryRepository
  -> WordInfo JSON + MediaAssetQueryService
  -> DictionaryReviewResponse
```

1. Load current `UserAccount`.
2. Sanitize `limit` to default `10`, minimum `1`, maximum `10`.
3. If `includeUpcoming=true`, load the next page of the user's `UserDictionaryEntry` rows ordered by `dueAt`.
4. Otherwise load only the user's due `UserDictionaryEntry` rows where `dueAt <= now`, ordered by `dueAt`.
5. For each entry, deserialize stored `WordInfoRecord.responseJson` to `WordInfoResponse` and derive the expected answer (`article + word` for nouns with article, otherwise word).
6. Map entry + word info + expected answer to `DictionaryReviewItem`.
7. For each item, optionally load latest completed `PronunciationAsset` for the word; if present, set pronunciation id and phrase.
8. If a pronunciation was found, optionally load latest completed `PhraseImageAsset` for the same word and phrase; if present, set phrase image id and URL.

### submitDictionaryReview — record one review answer and schedule the next review

```text
POST /api/v1/dictionary/review/{entryId}
  -> DictionaryController
  -> UserDictionaryService
  -> UserDictionaryEntryRepository
  -> WordInfo JSON
  -> DictionaryReviewSubmitResponse
```

1. Validate that request body contains `correct`.
2. Load current `UserAccount`.
3. Load `UserDictionaryEntry` by `entryId` and current user id.
4. Deserialize stored `WordInfoRecord.responseJson` to calculate the expected answer.
5. Increment repetition count and set `lastReviewedAt`.
6. If answer is correct, increase streak/ease factor and compute the next interval; if incorrect, reset streak, increment lapse count, reduce ease factor, and set interval to one day.
7. Set the next `dueAt` to the 04:00 UTC review-day boundary plus the interval.
8. Return review scheduling fields in `DictionaryReviewSubmitResponse`.

### getDictionaryVideos — build the current user's pronunciation-video cache manifest

```text
GET /api/v1/dictionary/videos
  -> DictionaryController
  -> UserDictionaryService
  -> UserDictionaryEntryRepository
  -> MediaAssetQueryService
  -> DictionaryVideoManifestResponse
```

1. Load current `UserAccount`.
2. Load all of the user's `UserDictionaryEntry` rows ordered by normalized word.
3. For each entry, take its `WordInfoRecord` and load the latest completed `PronunciationAsset`.
4. Drop entries without a completed pronunciation.
5. Map completed pronunciations to `DictionaryVideoManifestItem` DTOs.

## Media: pronunciations

### createPronunciation — queue or reuse pronunciation video generation

```text
POST /api/v1/media/pronunciations
  -> PronunciationController
  -> PronunciationService
  -> WordInfoRepository + UserDictionaryEntryRepository + PronunciationRepository
  -> async PronunciationGenerationProcessor
  -> PronunciationResponse
```

1. Validate and normalize request fields: `wordInfoId`, German `word`, German `phrase`, and language `de`.
2. Load `WordInfoRecord` by `wordInfoId`.
3. Ensure the current user has a `UserDictionaryEntry` for that word info record.
4. Search for an existing `PronunciationAsset` by word-info id and normalized phrase.
5. If an existing asset is not failed, reuse it and map it to `PronunciationResponse`.
6. If it is failed, reset it to `QUEUED`; otherwise create a new queued `PronunciationAsset`.
7. After transaction commit, start asynchronous generation; return the queued/reused asset mapped to `PronunciationResponse`.
8. Async generation later builds a pronunciation script, calls Google Veo, stores generated media in S3-compatible storage, optionally stores a compressed small video, and marks the asset completed or failed.

### getPronunciation — get pronunciation generation status

```text
GET /api/v1/media/pronunciations/{id}
  -> PronunciationController
  -> PronunciationService
  -> PronunciationRepository
  -> PronunciationResponse
```

1. Load `PronunciationAsset` by id.
2. Map asset status, metadata, URLs, and errors to `PronunciationResponse`.

### regeneratePronunciation — reject and replace a bad pronunciation video

```text
POST /api/v1/media/pronunciations/{id}/regenerate
  -> PronunciationController
  -> PronunciationService
  -> PronunciationRepository + UserDictionaryEntryRepository
  -> async PronunciationGenerationProcessor
  -> PronunciationResponse
```

1. Load `PronunciationAsset` by id.
2. Ensure the current user has a `UserDictionaryEntry` for the asset's `WordInfoRecord`.
3. Reset the asset to `QUEUED`, clear errors/completion time, save it, and queue async generation after commit.
4. Map the requeued asset to `PronunciationResponse`.

### getPronunciationVideo — stream the original generated pronunciation video

```text
GET /api/v1/media/pronunciations/{id}/video
  -> PronunciationController
  -> PronunciationService
  -> PronunciationRepository
  -> S3-compatible storage
  -> video bytes
```

1. Load `PronunciationAsset` by id.
2. Require status `COMPLETED` and a stored original video object key.
3. Read the video object from S3-compatible storage.
4. Stream bytes with stored content type, defaulting to `video/mp4`.

### getSmallPronunciationVideo — stream or lazily create the compressed pronunciation video

```text
GET /api/v1/media/pronunciations/{id}/video/small
  -> PronunciationController
  -> PronunciationService
  -> PronunciationRepository
  -> S3-compatible storage (+ compressor if missing)
  -> video bytes
```

1. Load `PronunciationAsset` by id.
2. Require status `COMPLETED` and an original video object key.
3. If `smallVideoObjectKey` exists, read and stream that object from S3-compatible storage.
4. Otherwise read the original video, attempt compression, store the small video if compression succeeds, save the new key on the asset, and stream the small video.
5. If compression cannot produce a small video, stream the original video instead.

## Media: phrase images

### createPhraseImage — queue or reuse phrase image generation

```text
POST /api/v1/media/phrase-images
  -> PronunciationController
  -> PhraseImageService
  -> WordInfoRepository + UserDictionaryEntryRepository + PhraseImageRepository
  -> async PhraseImageGenerationProcessor
  -> PhraseImageResponse
```

1. Validate and normalize request fields: `wordInfoId`, German `word`, German `phrase`, and language `de`.
2. Load `WordInfoRecord` by `wordInfoId`.
3. Ensure the current user has a `UserDictionaryEntry` for that word info record.
4. Search for an existing `PhraseImageAsset` by word-info id and normalized phrase.
5. If an existing asset is not failed, reuse it and map it to `PhraseImageResponse`.
6. If it is failed, reset it to `QUEUED`; otherwise create a new queued `PhraseImageAsset` with the configured prompt version.
7. After transaction commit, start asynchronous generation; return the queued/reused asset mapped to `PhraseImageResponse`.
8. Async generation later asks OpenAI for a visual scene description, builds an Imagen prompt, calls Google Imagen, stores generated image bytes in S3-compatible storage, and marks the asset completed or failed.

### getPhraseImage — get phrase image generation status

```text
GET /api/v1/media/phrase-images/{id}
  -> PronunciationController
  -> PhraseImageService
  -> PhraseImageRepository
  -> PhraseImageResponse
```

1. Load `PhraseImageAsset` by id.
2. Map asset status, metadata, URL, prompt text, and errors to `PhraseImageResponse`.

### regeneratePhraseImage — reject and replace a bad phrase image

```text
POST /api/v1/media/phrase-images/{id}/regenerate
  -> PronunciationController
  -> PhraseImageService
  -> PhraseImageRepository + UserDictionaryEntryRepository
  -> async PhraseImageGenerationProcessor
  -> PhraseImageResponse
```

1. Load `PhraseImageAsset` by id.
2. Ensure the current user has a `UserDictionaryEntry` for the asset's `WordInfoRecord`.
3. Reset the asset to `QUEUED`, update prompt version, clear errors/completion time, save it, and queue async generation after commit.
4. Map the requeued asset to `PhraseImageResponse`.

### getPhraseImageBytes — stream generated phrase image bytes

```text
GET /api/v1/media/phrase-images/{id}/image
  -> PronunciationController
  -> PhraseImageService
  -> PhraseImageRepository
  -> S3-compatible storage
  -> image bytes
```

1. Load `PhraseImageAsset` by id.
2. Require status `COMPLETED` and a stored image object key.
3. Read the image object from S3-compatible storage.
4. Stream bytes with stored content type, defaulting to `image/png`.

## Words

### getWordInfo — return cached or AI-generated compact German word information

```text
GET /api/v1/words/info?word=
  -> WordInfoController
  -> WordInfoService
  -> WordInfoRepository
  -> OpenAI chat model if cache miss
  -> WordInfoResponse
```

1. Trim/collapse whitespace, validate non-blank input, and enforce max length `80`.
2. Normalize query to lowercase.
3. Search `WordInfoRecord` by normalized query.
4. If cached, deserialize stored JSON to `WordInfoResponse` and set the record id.
5. On cache miss, call OpenAI chat through Spring AI with a JSON-schema response format to generate `ProviderWordInfo`.
6. Keep only the first three examples, fill noun article from gender when needed, and validate provider output.
7. Map `ProviderWordInfo` to API `WordInfoResponse`.
8. Store a new `WordInfoRecord` containing normalized query, normalized word, language, and serialized response JSON; on unique-key race, reload the existing record.
9. Return the API response.

### getWordSuggestions — autocomplete from cached word info and generated pronunciations

```text
GET /api/v1/words/suggestions?query=
  -> WordInfoController
  -> WordSuggestionService
  -> WordInfoRepository + PronunciationRepository
  -> WordSuggestionsResponse
```

1. Trim/collapse whitespace, validate non-blank input, and enforce max length `80`.
2. Load up to 10 cached `WordInfoRecord` rows whose normalized word contains the query, newest updated first.
3. Map each word-info record to a word suggestion and de-duplicate by normalized word.
4. Load up to 10 `PronunciationAsset` suggestions whose normalized word contains the query, newest updated first.
5. Map each pronunciation suggestion to a suggestion DTO and de-duplicate by normalized word + phrase.
6. Return suggestions in insertion order: cached word-info suggestions first, pronunciation suggestions second.

## Implementation review notes: redundancy, misuse, and smells

These notes are not endpoint behavior; they are review observations from comparing the OpenAPI operations with the current implementation.

```text
risk map
  security/ownership  -> highest priority
  side effects in GET -> surprising API semantics
  N+1 queries         -> performance concern
  duplicated flow     -> maintenance concern
```

### Cross-cutting smells

1. **Media status/stream endpoints are not user-scoped.**
   `getPronunciation`, `getPronunciationVideo`, `getSmallPronunciationVideo`, `getPhraseImage`, and `getPhraseImageBytes` load media assets only by asset id. They do not verify that the current user owns the media through a `UserDictionaryEntry` or otherwise has access to the linked `WordInfoRecord`. If media ids are guessable/leaked, one authenticated functional user may read another user's generated media/status.
2. **Regenerate endpoints can create dictionary entries as a side effect.**
   `regeneratePronunciation` and `regeneratePhraseImage` call `ensureEntryForCurrentUser(...)`. This means regenerating an existing asset both authorizes loosely and silently adds the word to the current user's dictionary if it was missing. That may be intended reuse behavior, but it is surprising for an endpoint described as “reject and replace”.
3. **Some GET endpoints can be expensive or mutating.**
   `getSmallPronunciationVideo` may compress and store a new small video during a GET request. That is convenient lazy caching, but it means GET has persistence side effects and can become CPU/storage-heavy.
4. **Review and manifest flows have N+1 query patterns.**
   `getDictionaryReview` loads entries, then for each entry queries latest pronunciation, and sometimes latest phrase image. `getDictionaryVideos` loads entries, then queries latest completed pronunciation per entry. For the current small limits this is likely acceptable for review, but the manifest can grow with dictionary size.
5. **Media creation flows duplicate almost identical validation/reuse/retry logic.**
   `createPronunciation` and `createPhraseImage` both normalize the same request shape, load `WordInfoRecord`, ensure dictionary entry, search by word-info + phrase, reuse non-failed assets, requeue failed assets, and queue async work. This is understandable now, but it is a maintenance smell if the behavior diverges accidentally.
6. **Global suggestions may leak generated pronunciation metadata.**
   `getWordSuggestions` searches all cached `WordInfoRecord` rows and pronunciation suggestions, not just the current user's dictionary. Global cached words may be intended, but generated pronunciation phrase/video metadata may be more user-specific than word-info cache data.

### Endpoint-specific review notes

| Endpoint | Observation | Suggested check |
| --- | --- | --- |
| `getDictionaryReview` | Meaningful flow, but enriches every review item with up to two extra media lookups. | Keep if limit stays capped at 10; consider batch media lookups if limit grows. |
| `getDictionaryVideos` | Manifest does one pronunciation lookup per dictionary entry. | Consider repository query that joins/filter completed pronunciations for all user dictionary words. |
| `createPronunciation` | Ensures dictionary entry before creating/reusing media. | Confirm this implicit “add to dictionary” behavior is desired for media generation. |
| `regeneratePronunciation` | Requeue is available to any functional user who knows the asset id, then adds linked word to their dictionary. | Add ownership/access check before allowing regeneration. |
| `getPronunciationVideo` / `getSmallPronunciationVideo` | Streams media by asset id without current-user ownership check. | Add current-user access check through linked word/dictionary entry or signed URLs with authorization rules. |
| `createPhraseImage` | Same shape as pronunciation creation. | Consider extracting shared normalization/reuse/retry helper only if future changes continue to duplicate. |
| `regeneratePhraseImage` | Same ownership/side-effect concern as pronunciation regeneration. | Add ownership/access check before requeue. |
| `getPhraseImageBytes` | Streams media by asset id without current-user ownership check. | Add current-user access check. |
| `getWordInfo` | Cache miss correctly calls OpenAI and stores response; no obvious meaningless step. | Confirm storing full API JSON is acceptable versus normalized columns for query/reporting needs. |
| `getWordSuggestions` | Merges global word cache and global pronunciations. | Decide whether suggestions should be global, current-user-only, or split by source. |

### Possible doc cleanups from this file itself

1. The filename intentionally follows the requested spelling, `endpoints_descptions.md`, but `descriptions` is misspelled. Rename only if the requested name was not intentional.
2. The endpoint sections are intentionally implementation-focused, so they repeat common steps like “load current user” and “map to response”. If the file becomes too long, move repeated auth/access behavior into the cross-cutting section and keep endpoint steps focused on entity/provider differences.
3. The ASCII diagrams are useful for scanning, but they are not a replacement for the numbered steps; if maintaining both becomes noisy, keep diagrams only for complex endpoints with repositories plus external providers.

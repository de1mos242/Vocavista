# Word Info Endpoint Plan

## Goal

Create the first API-first backend endpoint that accepts a German word and returns compact learning-oriented information for a user who knows English and Russian.

The first version should focus on useful vocabulary metadata without trying to become a full dictionary.

## Initial Assumptions

- Learning language: German.
- Known languages: English and Russian.
- Authentication, users, and user preferences do not exist yet.
- The backend will call OpenAI first, but the service boundary should allow adding other AI providers later.
- The first implementation is stateless and does not persist generated word information.

## Endpoint

Add an OpenAPI-defined endpoint:

```text
GET /api/v1/words/info?word=Hausaufgabe
```

Request shape: no request body. Pass the word as a required query parameter named `word`.

Decision: use `GET` with a query parameter instead of a path variable to avoid path-segment escaping issues for spaces, punctuation, umlauts, and fixed expressions. Do not include `learningLanguage` or `translationLanguages` in each request. Until user preferences exist, the backend assumes German as the learning language and English plus Russian as known languages. Later, these language settings should come from user preferences.

Response shape, draft:

```json
{
  "normalizedWord": "Hausaufgabe",
  "language": "de",
  "translations": {
    "en": ["homework", "assignment"],
    "ru": ["домашнее задание"]
  },
  "partOfSpeech": "noun",
  "gender": "feminine",
  "article": "die",
  "plural": "Hausaufgaben",
  "frequency": "common",
  "isCompound": true,
  "compoundParts": [
    {
      "word": "Haus",
      "meanings": {
        "en": ["house"],
        "ru": ["дом"]
      }
    },
    {
      "word": "Aufgabe",
      "meanings": {
        "en": ["task", "assignment"],
        "ru": ["задание"]
      }
    }
  ],
  "shortNote": {
    "en": ["A standard word for school homework or assigned tasks."],
    "ru": ["Обычное слово для школьного домашнего задания или назначенных задач."]
  },
  "examples": [
    {
      "sentence": "Ich mache meine Hausaufgabe nach dem Abendessen.",
      "translations": {
        "en": ["I do my homework after dinner."],
        "ru": ["Я делаю домашнее задание после ужина."]
      }
    },
    {
      "sentence": "Die Hausaufgabe ist heute ziemlich leicht.",
      "translations": {
        "en": ["The homework is quite easy today."],
        "ru": ["Сегодня домашнее задание довольно легкое."]
      }
    },
    {
      "sentence": "Hast du die Hausaufgabe schon fertig?",
      "translations": {
        "en": ["Have you already finished the homework?"],
        "ru": ["Ты уже закончил домашнее задание?"]
      }
    }
  ]
}
```

## OpenAPI Contract

Create `backend/src/main/resources/openapi/vocavista-api.yaml` with:

- Required `word` query parameter on `GET /api/v1/words/info`.
- `WordInfoResponse` schema.
- `normalizedWord` field for the lemma or base form of the requested word.
- `translations` object keyed by language code, with string language codes for now.
- `WordExample` schema with German sentence plus language-keyed translations only.
- `PartOfSpeech` enum with common vocabulary-oriented values.
- `WordFrequency` enum with `rare`, `uncommon`, `common`, and `very_common`.
- `Gender` enum with `masculine`, `feminine`, and `neuter`.
- `GermanArticle` enum with `der`, `die`, and `das` for the first German-only version.
- German noun fields should include `gender`, `article`, and `plural` when relevant.
- `shortNote` as a language-keyed object, using the same shape as `translations`.
- `CompoundPart` schema with the part text plus language-keyed meanings.
- Error responses for validation and provider failures.

Recommended validation rules:

- `word` is required.
- Trimmed `word` length: 1 to 80 characters.
- The endpoint accepts a single word or fixed expression for now, not a full sentence.
- The request must not expose language-selection fields in the first version.
- `partOfSpeech` must be one of `noun`, `verb`, `adjective`, `adverb`, `pronoun`, `preposition`, `conjunction`, `interjection`, `phrase`, or `other`.
- `frequency` must be one of `rare`, `uncommon`, `common`, or `very_common`.
- `translations` uses language-code keys such as `en` and `ru`. These can be constrained later if needed.
- `normalizedWord` should contain the lemma or base form. For an already normalized input, it can match the requested query value.
- `gender` must be one of `masculine`, `feminine`, or `neuter` for nouns, and empty for non-nouns.
- `article` must be one of `der`, `die`, or `das` for German nouns, and empty for non-nouns.
- `plural` is a noun-only field and should be empty for non-nouns.
- `compoundParts` is an empty array when `isCompound` is `false`.

## OpenAPI Generator

Add OpenAPI Generator to the Maven build so the backend is api-first:

- Generate Spring MVC API interfaces from `vocavista-api.yaml`.
- Generate DTO model classes from the OpenAPI schemas.
- Implement the generated API interface in a Spring controller class.
- Keep generated sources under `target/generated-sources/openapi`.
- Configure Maven so generated sources compile automatically.

Likely Maven plugin:

```text
org.openapitools:openapi-generator-maven-plugin
```

Initial generator direction:

- Generator: `spring`.
- Library: Spring Boot / Spring MVC compatible.
- Interface-only API generation if possible.
- Use Bean Validation annotations from the OpenAPI schema.

## Backend Design

Introduce a small provider-independent service boundary:

```text
WordInfoController -> WordInfoService -> AiWordInfoProvider -> OpenAiWordInfoProvider
```

Responsibilities:

- `WordInfoController`: implements generated OpenAPI interface and delegates to the service.
- `WordInfoService`: validates business assumptions, builds provider request, maps provider output into API response.
- `AiWordInfoProvider`: internal interface for word-info generation.
- `OpenAiWordInfoProvider`: calls OpenAI using Spring `RestClient`.

Configuration:

- `spring.ai.openai.api-key` for the API key.
- `spring.ai.openai.chat.model` with a sensible default, for example `gpt-4.1-mini` or the current preferred small structured-output model.
- Spring AI's OpenAI base URL property when the default OpenAI endpoint is not used.

## AI Prompting

The provider request should send:

- The input word.
- Fixed context: learning German, known languages English and Russian.
- A strict instruction to return compact JSON matching the API response shape.
- Requirements for exactly 3 German example sentences.
- Requirements for only sentence translations in examples, with no per-example usage notes. Example translations use the same language-keyed object shape as top-level `translations`.
- Requirements for compound-part meanings in both English and Russian when the word is compound, using the same language-keyed object shape as `translations`.
- Requirements to keep notes short, translated into known languages, and avoid over-explaining.

Provider response handling:

- Prefer structured JSON output if the selected OpenAI API/model supports it cleanly.
- Parse and validate required fields before returning to the client.
- Return a controlled backend error if the provider response is malformed.

## Error Handling

Initial error cases:

- `400 Bad Request`: missing, blank, or too-long word.
- `502 Bad Gateway`: AI provider returns malformed content or a non-success response.
- `503 Service Unavailable`: AI provider is unavailable or times out.

The first version should avoid exposing provider internals or raw prompts in client responses.

## Implementation Steps

1. Add the OpenAPI YAML contract for `GET /api/v1/words/info` with required `word` query parameter.
2. Add OpenAPI Generator Maven configuration.
3. Generate API interfaces and DTOs as part of the build.
4. Implement the generated API interface with a controller.
5. Add `WordInfoService` and the `AiWordInfoProvider` interface.
6. Add `OpenAiWordInfoProvider` using `RestClient` and application configuration properties.
7. Add prompt construction and provider JSON parsing.
8. Add validation and error mapping.
9. Add tests for request validation, service mapping, and provider-response parsing.
10. Run `./mvnw test` and confirm the generated API compiles.

## Decisions

- The first endpoint is `GET /api/v1/words/info`.
- The word is passed as a required `word` query parameter.
- There is no request body for the first version.
- Response does not echo the requested `word` value. It includes only `normalizedWord` for the lemma or base form, useful for inflected inputs such as German plurals or conjugated verbs.
- Learning and known languages are not request parameters in the first version.
- Before user preferences exist, the backend assumes German as the learning language and English plus Russian as known languages.
- Later, language settings should come from user preferences.
- `frequency` is an enum with values `rare`, `uncommon`, `common`, and `very_common`.
- Examples contain only the German sentence and language-keyed translations. Do not add per-example usage notes in the first version.
- The first version is stateless. Do not store generated responses in PostgreSQL until the response shape and product behavior are more stable.
- Translations use a language-keyed object, for example `{ "en": ["homework"], "ru": ["домашнее задание"] }`. Language keys are strings for now, with possible constraints later.
- `shortNote` uses the same language-keyed object shape as `translations`.
- Example sentence translations use the same language-keyed object shape so adding languages does not require adding new API fields.
- `partOfSpeech` is an enum with values `noun`, `verb`, `adjective`, `adverb`, `pronoun`, `preposition`, `conjunction`, `interjection`, `phrase`, and `other`.
- German nouns include `gender`, `article`, and `plural` when relevant, for example `feminine`, `die`, and `Hausaufgaben`.
- `gender` is an enum with values `masculine`, `feminine`, and `neuter`. For non-nouns, gender is empty. In OpenAPI implementation, prefer nullable or omitted fields over an empty string unless we explicitly decide otherwise.
- `article` is a German-specific enum with values `der`, `die`, and `das` for now. This may need redesign when supporting learning languages beyond German.
- `article` and `plural` are empty for non-nouns. In OpenAPI implementation, prefer nullable or omitted fields over empty strings.
- Compound parts include language-keyed `meanings`, using the same shape as `translations`, for example `{ "en": ["house"], "ru": ["дом"] }`.
- For non-compound words, `compoundParts` is an empty array, not `null` or omitted.

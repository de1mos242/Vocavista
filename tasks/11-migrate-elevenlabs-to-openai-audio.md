# Issue 11: Migrate ElevenLabs to OpenAI Audio

- Issue: https://github.com/de1mos242/Vocavista/issues/11
- Task root: `tasks/11-migrate-pronunciation-audio-generation-from-elevenlabs-to-openai/`
- Branch: `11-migrate-pronunciation-audio-generation-from-elevenlabs-to-openai`

## Goal

Replace ElevenLabs pronunciation audio generation with OpenAI speech generation while keeping the existing cached audio asset flow, same-origin playback URL, and browser-side TalkingHead rendering.

## Starting State

- `ElevenLabsTextToSpeechProvider` is the only production `TextToSpeechProvider`.
- A pronunciation asset stores generated audio in S3-compatible storage and records `audioProvider` plus `audioModel` metadata.
- Cache keys include language, word-info ID, normalized word, normalized phrase, script template version, voice config, provider name, and model name.
- The current provider makes three TTS requests: first isolated word slower, second isolated word faster, then phrase at medium speed.
- Local runtime docs currently require `VOCAVISTA_MEDIA_ELEVENLABS_API_KEY` in addition to `SPRING_AI_OPENAI_API_KEY`.

## Implemented State

- `OpenAiTextToSpeechProvider` is the only production `TextToSpeechProvider`.
- The provider uses the official OpenAI Java SDK speech API with `gpt-4o-mini-tts`, default voice `coral`, and default `mp3` output.
- The OpenAI audio provider uses `vocavista.media.openai.api-key`, which defaults to the existing `spring.ai.openai.api-key` so local runtime only needs `SPRING_AI_OPENAI_API_KEY`.
- The Spring `local` profile is active by default so `application-local.yaml` overrides are loaded automatically when present.
- Cache keys and persisted audio metadata identify provider `openai` and include model, voice, response format, and instructions through `modelName()`.
- The pronunciation script is generated once and sent with instructions to speak clear German for a language learner: target word slowly, target word naturally, then the full phrase.
- Word-info provider results now retain the raw OpenAI response string alongside the parsed DTO. Validation and parsing failures include `rawProviderResponse` in the thrown exception message and log it through the existing bad-gateway error handler.

## OpenAI Options

| Option | Pricing | Fit for Vocavista |
| --- | --- | --- |
| `gpt-4o-mini-tts` Speech API | `$0.60 / 1M text tokens` input plus `$12 / 1M audio tokens` output | Best target. Current OpenAI TTS model, supports promptable tone, accent, speed, intonation, German text, streaming, and `mp3`/`opus`/`wav`/`pcm`. |
| `tts-1` Speech API | `$15 / 1M characters` | Cheapest/simple fallback if quality is acceptable. Less controllable than `gpt-4o-mini-tts`. |
| `tts-1-hd` Speech API | `$30 / 1M characters` | Higher-quality legacy fallback, still less controllable than `gpt-4o-mini-tts`. |
| Realtime models | `gpt-realtime-mini`: text `$0.60` input, `$2.40` output per 1M tokens; audio input/output token pricing applies for full realtime audio | Not a good fit. Vocavista generates stored pronunciation assets, not live conversational audio. |
| Custom OpenAI voices | Sales/eligible customers only | Not needed now. Built-in voices should be tested first. |

## Pricing Notes For Our Case

- Each pronunciation asset is a short German script: word twice plus one phrase. The enforced maximum is roughly 405 input characters, but normal assets are likely much smaller.
- OpenAI `gpt-4o-mini-tts` input text cost should be negligible for this workload; generated audio duration dominates cost.
- If clips average 6-12 seconds, 1,000 generated pronunciation assets are roughly 100-200 audio minutes. Using a common audio-token estimate, `gpt-4o-mini-tts` should land around `$5-$11` per 1,000 assets, plus a very small text-token component. Verify exact conversion in the OpenAI usage dashboard after sample generation.
- `tts-1` would cost about `$1.80-$3.30` per 1,000 assets at 120-220 generated characters each; `tts-1-hd` doubles that. This is cheaper, but likely worse for our need to control pronunciation teaching style.
- ElevenLabs current paid tiers list about `$0.17-$0.20` per generated minute after the free tier. At 100-200 minutes per 1,000 assets, that is roughly `$17-$40` per 1,000 assets before considering subscription tier limits. On this basis, OpenAI should reduce ongoing audio-generation cost.

## Recommended Direction

- Implement an `OpenAiTextToSpeechProvider` using the official OpenAI Java SDK speech API and default it to `gpt-4o-mini-tts`.
- Start with one request using the existing `PronunciationScript.text()` and instructions that ask for: clear German pronunciation, first word slow with pause, second word normal/confident, then the phrase naturally.
- If playback quality loses the current segment-speed behavior, fall back to three OpenAI speech requests with segment-specific instructions and join the outputs as the current provider does.
- Default output to `mp3` for compatibility with existing storage/playback unless TalkingHead/browser latency testing shows `wav` is materially better.
- Include OpenAI model, voice, response format, and instruction/template version in `modelName()` so cached audio is regenerated when voice settings change.

## Implementation Notes

- Remove or disable ElevenLabs production configuration after OpenAI generation is working.
- Use the official OpenAI Java SDK directly for speech generation because the current SDK supports `SpeechCreateParams` with model, voice, instructions, and response format.
- Keep provider exceptions mapped to `MediaGenerationException` codes consistent with existing behavior.
- Update `README.md`, `.env.example`, `application-local.example.yaml`, and architecture/spec docs from ElevenLabs to OpenAI.
- Update provider tests to assert SDK speech params, output content type, missing API key, provider errors, and empty audio handling.

## Remaining Follow-Up

- Default voice is currently `coral` after `marin` sounded too flat in real usage. It still needs a quick German listening test against other OpenAI voices.
- If OpenAI remains noticeably less alive than ElevenLabs after voice/instruction tuning, reintroducing ElevenLabs as the selected production provider may be worth the extra configuration complexity.
- Should we preserve the exact three-segment speed behavior or accept a single prompt-controlled request if it sounds good enough?

## Progress

- 2026-05-24: Created issue and task worktree. Reviewed current ElevenLabs provider, OpenAI Speech API options, and pricing. Recommended `gpt-4o-mini-tts` as the migration target with `tts-1` as a cheap fallback if quality is acceptable.
- 2026-05-24: Decided not to keep both providers because clean provider selection would require extra conditional beans and duplicate configuration. Replaced the production TTS provider with a single OpenAI `/v1/audio/speech` implementation defaulting to `gpt-4o-mini-tts`, `marin`, and `mp3`; it reuses `SPRING_AI_OPENAI_API_KEY` through `vocavista.media.openai.api-key`.
- 2026-05-25: Updated local configuration, environment examples, README files, and current architecture/spec docs to describe OpenAI audio generation instead of ElevenLabs. Added OpenAI provider tests covering successful request shape, missing API key, provider errors, and empty audio.
- 2026-05-25: Set the Spring `local` profile active by default in `application.yaml` so local overrides do not require passing `-Dspring-boot.run.profiles=local`.
- 2026-05-25: Added raw OpenAI word-info response propagation for malformed provider content. `AiWordInfoProvider` now returns both parsed `ProviderWordInfo` and raw response text; validation errors include raw response text in `AiProviderBadGatewayException.providerResponse()` and logs label it as `rawProviderResponse`.
- 2026-05-25: Changed malformed word-info exceptions to include `rawProviderResponse=...` directly in the exception message so plain stack traces show the raw model output, not only the error-handler log line.
- 2026-05-25: Tuned OpenAI TTS defaults toward a livelier sound after real usage showed the default was less alive than ElevenLabs. Changed default voice from `marin` to `coral` and made the default instructions explicitly request warm, expressive, conversational German.
- 2026-05-25: Replaced the initial raw `RestClient` speech call with the official OpenAI Java SDK. Added `com.openai:openai-java` as a direct dependency, built `SpeechCreateParams` through the SDK, and updated provider tests to assert SDK request params instead of HTTP JSON.

## Verification

- 2026-05-25: `./mvnw -Dtest=OpenAiTextToSpeechProviderTest,PronunciationGenerationProcessorTest,PronunciationServiceTest test` passed; 10 focused tests passed.
- 2026-05-25: `./mvnw test` passed; 32 tests passed.
- 2026-05-25: `./mvnw test` passed again after setting the `local` profile active by default; 32 tests passed.
- 2026-05-25: `./mvnw -Dtest=SpringAiOpenAiWordInfoProviderTest,WordInfoServiceTest,WordInfoControllerTest test` passed after adding raw response logging; 16 targeted tests passed.
- 2026-05-25: `./mvnw test` passed after adding raw response logging; 32 tests passed.
- 2026-05-25: `./mvnw -Dtest=SpringAiOpenAiWordInfoProviderTest,WordInfoServiceTest test` passed after adding `rawProviderResponse` to exception messages; 9 targeted tests passed.
- 2026-05-25: `./mvnw test` passed after adding `rawProviderResponse` to exception messages; 32 tests passed.
- 2026-05-25: `./mvnw -Dtest=OpenAiTextToSpeechProviderTest test` passed after tuning the default OpenAI TTS voice/instructions; 4 targeted tests passed.
- 2026-05-25: `./mvnw -Dtest=OpenAiTextToSpeechProviderTest test` passed after replacing raw HTTP with the OpenAI Java SDK; 4 targeted tests passed.
- 2026-05-25: `./mvnw test` passed after replacing raw HTTP with the OpenAI Java SDK; 32 tests passed.

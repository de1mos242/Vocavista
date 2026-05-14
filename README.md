# Vocavista

Vocavista is a language learning application that combines spaced repetition with context-rich audiovisual vocabulary practice. Instead of teaching words in isolation, it presents vocabulary through realistic usage scenarios, natural speech, lip articulation, and matching visual context.

## Core Idea

The app helps learners understand and remember vocabulary by showing:

- Correct pronunciation and articulation through lip-synced talking-head video.
- Natural audio with the target word repeated before a contextual sentence.
- Visual context that clarifies meaning and nuance, such as the difference between similar verbs like "walk" and "run".
- Spaced repetition flows inspired by Anki to reinforce long-term retention.

## Media Strategy

Vocavista uses a hybrid AI media-generation pipeline. Media is generated once per vocabulary word and cached centrally, so all users can reuse the same generated assets without repeated latency or cost.

The planned pipeline uses:

- An LLM as a content director for example sentence and prompt generation.
- Natural speech synthesis via ElevenLabs or OpenAI TTS.
- Lip-sync video for isolated word pronunciation.
- Static AI-generated context images for the example sentence.
- Text-to-video only for highly dynamic words that cannot be explained well with a static image.

## Technical Direction

- Backend: Spring Boot 4 Java application with a native build target.
- Database: PostgreSQL for user progress, vocabulary metadata, and generated media metadata.
- Infrastructure: AWS cloud deployment managed with Terraform as Infrastructure as Code.
- Initial hosting: no Kubernetes; start with simpler AWS-managed services and keep the architecture deployable by Terraform.
- Storage: S3-compatible object storage for cached generated audio, video, and image assets.
- Frontend: React Native with web support where practical, preferably through Expo and React Native Web. If media playback or platform constraints make this too costly, start with a focused React web version first.

## Documentation

The full initial specification is available in [`documentation/app-specification.md`](documentation/app-specification.md).

The current technical architecture direction is documented in [`documentation/technical-architecture.md`](documentation/technical-architecture.md).

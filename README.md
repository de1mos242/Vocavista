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

- Backend services and AI integrations in Python or Java.
- Kubernetes-based deployment for scalable delivery.
- Relational database for user progress and vocabulary metadata.
- Cloud object storage for cached audio and video assets.
- Frontend support for spaced repetition and seamless audiovisual playback.

## Documentation

The full initial specification is available in [`documentation/app-specification.md`](documentation/app-specification.md).

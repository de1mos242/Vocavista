# App Specification: Vocavista

## 1. Core Concept & Objectives

Vocavista is a language learning app that combines spaced repetition (similar to Anki) with highly context-driven, audiovisual content. The focus is on teaching vocabulary not in isolation, but in realistic application scenarios. The user should see the exact articulation (lip movements) and immediately grasp nuances (e.g., "walk" vs. "run") through combined visual and auditory cues.

## 2. Media & AI Strategy (Hybrid Approach)

To avoid high latency and control costs, media is generated **per word** (not per user) and cached in a central database for reuse across all users.

### 2.1 LLM Direction (Content Generation)

* An LLM (e.g., Gemini 1.5 Flash) acts as the "director."
* For each new word, the LLM generates a differentiated example sentence that explains the context.
* The LLM controls the workflow and generates the prompts for the image and audio APIs.

### 2.2 Audio (Natural Speech Synthesis)

* **Models:** ElevenLabs or OpenAI TTS (focusing on maximum naturalness, strictly avoiding robotic voices).
* **Format:** The script requires the target word to be pronounced twice with pauses, followed by the example sentence.
* **Structure:** `{Word}. <Pause> {Word}. <Pause> {Context sentence}.`

### 2.3 Visual Layer (Talking Head + Static Context)

* **Part 1: Articulation (Lip-Sync):** The app uses a lip-sync API (such as D-ID or HeyGen) to display a talking head while the first two isolated words are spoken. This teaches the user the correct mouth movements.
* **Part 2: Context Image (Scene Transition):** As soon as the example sentence begins in the audio track, the video crossfades to a matching, static image. This image is generated cost-effectively using **Nano Banana 2** (Gemini 3 Flash Image).
* *Exception:* Expensive text-to-video models (like Veo) are exclusively reserved for highly dynamic or otherwise hard-to-explain verbs.

## 3. Technical Architecture

### 3.1 Backend & Infrastructure

* **Logic & APIs:** Implementation of backend services and AI API integrations in **Python** or **Java**.
* **Hosting:** Deployment via a **Kubernetes** cluster for scalable content delivery.
* **Database & Storage:** Relational database (e.g., RDS) for user progress and vocabulary metadata. Cloud storage (S3/GCS) for caching the generated `.mp4` and `.mp3` files.

### 3.2 Frontend

* The frontend manages the spaced repetition algorithm either locally or via backend synchronization.
* Smooth media playback: Ensures a seamless transition between the lip-sync video and the overlaid context image while the audio track continues without interruption.

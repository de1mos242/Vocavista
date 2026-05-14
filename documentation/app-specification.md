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

* **Logic & APIs:** Implementation of backend services and AI API integrations in **Java** using **Spring Boot 4**.
* **Native Build:** The backend should support a native build target for lower runtime overhead and faster startup.
* **Hosting:** Initial deployment should avoid Kubernetes. The first production direction is AWS-managed infrastructure configured with Terraform.
* **Database & Storage:** PostgreSQL for user progress and vocabulary metadata. AWS S3 for caching generated `.mp4`, `.mp3`, and image files.
* **Infrastructure as Code:** Terraform should define cloud resources so the environment can be recreated and evolved safely.

### 3.2 Frontend

* The frontend manages the spaced repetition algorithm either locally or via backend synchronization.
* Web frontend should be built with React or React Native Web.
* Mobile frontend should be built with React Native for iOS and Android.
* Preferred direction: use React Native with web support where practical, likely through Expo and React Native Web, to maximize shared code across web, iOS, and Android.
* Smooth media playback: Ensures a seamless transition between the lip-sync video and the overlaid context image while the audio track continues without interruption.

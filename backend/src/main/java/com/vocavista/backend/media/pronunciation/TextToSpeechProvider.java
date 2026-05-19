package com.vocavista.backend.media.pronunciation;

interface TextToSpeechProvider {

	GeneratedAudio generate(PronunciationScript script);

	String providerName();

	String modelName();

}

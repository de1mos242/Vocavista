package com.vocavista.backend.media.pronunciation;

interface LipSyncVideoProvider {

	GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio);

	String providerName();

	String modelName();

}

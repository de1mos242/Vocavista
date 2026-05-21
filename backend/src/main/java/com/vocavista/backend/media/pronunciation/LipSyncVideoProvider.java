package com.vocavista.backend.media.pronunciation;

import java.net.URI;

interface LipSyncVideoProvider {

	GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio, URI audioUrl);

	String providerName();

	String modelName();

}

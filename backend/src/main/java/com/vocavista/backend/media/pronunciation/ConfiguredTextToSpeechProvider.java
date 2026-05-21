package com.vocavista.backend.media.pronunciation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${vocavista.media.provider-mode:fake}' == 'real' and '${vocavista.media.tts-provider:fake}' != 'elevenlabs'")
class ConfiguredTextToSpeechProvider implements TextToSpeechProvider {

	private final String provider;

	ConfiguredTextToSpeechProvider(@Value("${vocavista.media.tts-provider}") String provider) {
		this.provider = provider;
	}

	@Override
	public GeneratedAudio generate(PronunciationScript script) {
		throw new MediaGenerationException("tts_provider_not_implemented",
				"Real text-to-speech provider is configured but not implemented yet: " + provider);
	}

	@Override
	public String providerName() {
		return provider;
	}

	@Override
	public String modelName() {
		return "configured";
	}

}

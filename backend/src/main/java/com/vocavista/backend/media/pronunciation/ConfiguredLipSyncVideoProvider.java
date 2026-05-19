package com.vocavista.backend.media.pronunciation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "provider-mode", havingValue = "real")
class ConfiguredLipSyncVideoProvider implements LipSyncVideoProvider {

	private final String provider;

	ConfiguredLipSyncVideoProvider(@Value("${vocavista.media.lipsync-provider}") String provider) {
		this.provider = provider;
	}

	@Override
	public GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio) {
		throw new MediaGenerationException("lipsync_provider_not_implemented",
				"Real lip-sync provider is configured but not implemented yet: " + provider);
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

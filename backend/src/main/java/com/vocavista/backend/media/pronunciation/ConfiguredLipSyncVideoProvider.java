package com.vocavista.backend.media.pronunciation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${vocavista.media.provider-mode:fake}' == 'real' and '${vocavista.media.lipsync-provider:fake}' != 'did'")
class ConfiguredLipSyncVideoProvider implements LipSyncVideoProvider {

	private final String provider;

	ConfiguredLipSyncVideoProvider(@Value("${vocavista.media.lipsync-provider}") String provider) {
		this.provider = provider;
	}

	@Override
	public GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio, java.net.URI audioUrl) {
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

package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "provider-mode", havingValue = "fake", matchIfMissing = true)
class FakeLipSyncVideoProvider implements LipSyncVideoProvider {

	@Override
	public GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio, URI audioUrl) {
		String content = "fake-video\n" + script.text() + "\naudio-bytes=" + audio.bytes().length + "\naudio-url="
				+ audioUrl;
		return new GeneratedVideo(content.getBytes(StandardCharsets.UTF_8), "text/plain");
	}

	@Override
	public String providerName() {
		return "fake";
	}

	@Override
	public String modelName() {
		return "fake-lipsync-v1";
	}

}

package com.vocavista.backend.media.pronunciation;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "provider-mode", havingValue = "fake", matchIfMissing = true)
class FakeTextToSpeechProvider implements TextToSpeechProvider {

	@Override
	public GeneratedAudio generate(PronunciationScript script) {
		String content = "fake-audio\n" + script.text();
		return new GeneratedAudio(content.getBytes(StandardCharsets.UTF_8), "text/plain");
	}

	@Override
	public String providerName() {
		return "fake";
	}

	@Override
	public String modelName() {
		return "fake-tts-v1";
	}

}

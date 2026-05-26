package com.vocavista.backend.media.pronunciation;

import com.openai.errors.OpenAIServiceException;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Component;

@Component
class PronunciationAudioGenerator {

	private final TextToSpeechModel speechModel;

	PronunciationAudioGenerator(TextToSpeechModel speechModel) {
		this.speechModel = speechModel;
	}

	public GeneratedAudio generate(PronunciationScript script) {
		byte[] bytes;
		try {
			TextToSpeechResponse response = speechModel.call(new TextToSpeechPrompt(script.text()));
			bytes = response.getResult().getOutput();
		}
		catch (OpenAIServiceException ex) {
			throw new MediaGenerationException("tts_provider_error",
					"OpenAI returned HTTP " + ex.statusCode() + ": " + ex.body(), ex);
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI speech generation failed", ex);
		}

		if (bytes.length == 0) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI returned empty audio");
		}
		return new GeneratedAudio(bytes, contentTypeFor(speechModel.getDefaultOptions().getFormat()));
	}

	public String providerName() {
		return "openai";
	}

	public String modelName() {
		TextToSpeechOptions options = speechModel.getDefaultOptions();
		return "%s:%s:%s:%s".formatted(options.getModel(), options.getVoice(), options.getFormat(), "script-text");
	}

	private static String contentTypeFor(String responseFormat) {
		return switch (responseFormat) {
			case "aac" -> "audio/aac";
			case "flac" -> "audio/flac";
			case "opus" -> "audio/opus";
			case "pcm" -> "audio/pcm";
			case "wav" -> "audio/wav";
			default -> "audio/mpeg";
		};
	}

}

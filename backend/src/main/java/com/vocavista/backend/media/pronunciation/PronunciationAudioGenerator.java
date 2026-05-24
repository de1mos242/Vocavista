package com.vocavista.backend.media.pronunciation;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.HttpResponse;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.audio.speech.SpeechCreateParams;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class PronunciationAudioGenerator {

	private static final String MISSING_API_KEY = "__missing__";

	private final SpeechGenerator speechGenerator;
	private final OpenAiTextToSpeechProperties properties;

	@Autowired
	PronunciationAudioGenerator(OpenAiTextToSpeechProperties properties) {
		this(createClient(properties).audio().speech()::create, properties);
	}

	PronunciationAudioGenerator(SpeechGenerator speechGenerator, OpenAiTextToSpeechProperties properties) {
		this.speechGenerator = speechGenerator;
		this.properties = properties;
	}

	public GeneratedAudio generate(PronunciationScript script) {
		if (!StringUtils.hasText(properties.getApiKey()) || MISSING_API_KEY.equals(properties.getApiKey())) {
			throw new MediaGenerationException("tts_provider_not_configured", "OpenAI API key is not configured");
		}

		byte[] bytes;
		try (HttpResponse response = speechGenerator.create(requestFor(script))) {
			bytes = response.body().readAllBytes();
		}
		catch (OpenAIIoException | IOException ex) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI speech generation failed", ex);
		}
		catch (OpenAIServiceException ex) {
			throw new MediaGenerationException("tts_provider_error",
					"OpenAI returned HTTP " + ex.statusCode() + ": " + ex.body(), ex);
		}

		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI returned empty audio");
		}
		return new GeneratedAudio(bytes, contentTypeFor(properties.getResponseFormat()));
	}

	public String providerName() {
		return "openai";
	}

	public String modelName() {
		return "%s:%s:%s:%s:%s".formatted(properties.getModel(), properties.getVoice(),
				properties.getResponseFormat(), properties.getInstructions(), "script-text");
	}

	private SpeechCreateParams requestFor(PronunciationScript script) {
		return SpeechCreateParams.builder()
				.model(properties.getModel())
				.voice(properties.getVoice())
				.input(script.text())
				.instructions(properties.getInstructions())
				.responseFormat(SpeechCreateParams.ResponseFormat.of(properties.getResponseFormat()))
				.build();
	}

	private static OpenAIClient createClient(OpenAiTextToSpeechProperties properties) {
		return OpenAIOkHttpClient.builder()
				.apiKey(properties.getApiKey())
				.baseUrl(properties.getBaseUrl())
				.build();
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

	@FunctionalInterface
	interface SpeechGenerator {

		HttpResponse create(SpeechCreateParams params);

	}

}

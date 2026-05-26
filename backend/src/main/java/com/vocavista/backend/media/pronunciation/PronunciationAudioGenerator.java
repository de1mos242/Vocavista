package com.vocavista.backend.media.pronunciation;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
class PronunciationAudioGenerator {

	private static final String MISSING_API_KEY = "__missing__";
	private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

	private final OpenAiTextToSpeechProperties properties;
	private final RestClient restClient;

	PronunciationAudioGenerator(OpenAiTextToSpeechProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(OPENAI_BASE_URL).build();
	}

	public GeneratedAudio generate(PronunciationScript script) {
		if (!StringUtils.hasText(properties.getApiKey()) || MISSING_API_KEY.equals(properties.getApiKey())) {
			throw new MediaGenerationException("tts_provider_not_configured", "OpenAI API key is not configured");
		}

		byte[] bytes;
		try {
			bytes = restClient.post()
					.uri("/audio/speech")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_OCTET_STREAM)
					.body(requestFor(script))
					.retrieve()
					.body(byte[].class);
		}
		catch (RestClientResponseException ex) {
			throw new MediaGenerationException("tts_provider_error",
					"OpenAI returned HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString(), ex);
		}
		catch (RestClientException ex) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI speech generation failed", ex);
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

	private Map<String, String> requestFor(PronunciationScript script) {
		return Map.of(
				"model", properties.getModel(),
				"voice", properties.getVoice(),
				"input", script.text(),
				"instructions", properties.getInstructions(),
				"response_format", properties.getResponseFormat());
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

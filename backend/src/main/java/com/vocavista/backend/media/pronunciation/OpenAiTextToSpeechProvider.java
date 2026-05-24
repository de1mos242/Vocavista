package com.vocavista.backend.media.pronunciation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
class OpenAiTextToSpeechProvider implements TextToSpeechProvider {

	private static final String MISSING_API_KEY = "__missing__";

	private final RestClient restClient;
	private final OpenAiTextToSpeechProperties properties;

	@Autowired
	OpenAiTextToSpeechProvider(
			RestClient.Builder restClientBuilder,
			OpenAiTextToSpeechProperties properties) {
		this(restClientBuilder.baseUrl(properties.getBaseUrl()).build(), properties);
	}

	OpenAiTextToSpeechProvider(RestClient restClient, OpenAiTextToSpeechProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public GeneratedAudio generate(PronunciationScript script) {
		if (!StringUtils.hasText(properties.getApiKey()) || MISSING_API_KEY.equals(properties.getApiKey())) {
			throw new MediaGenerationException("tts_provider_not_configured", "OpenAI API key is not configured");
		}

		byte[] bytes = restClient.post()
				.uri("/v1/audio/speech")
				.header("Authorization", "Bearer " + properties.getApiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestBody(script))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new MediaGenerationException("tts_provider_error", "OpenAI returned HTTP "
							+ response.getStatusCode().value() + ": " + readBody(response.getBody()));
				})
				.body(byte[].class);

		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("tts_provider_error", "OpenAI returned empty audio");
		}
		return new GeneratedAudio(bytes, contentTypeFor(properties.getResponseFormat()));
	}

	@Override
	public String providerName() {
		return "openai";
	}

	@Override
	public String modelName() {
		return "%s:%s:%s:%s:%s".formatted(properties.getModel(), properties.getVoice(),
				properties.getResponseFormat(), properties.getInstructions(), "script-text");
	}

	private Map<String, Object> requestBody(PronunciationScript script) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.getModel());
		body.put("voice", properties.getVoice());
		body.put("input", script.text());
		body.put("instructions", properties.getInstructions());
		body.put("response_format", properties.getResponseFormat());
		return body;
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

	private static String readBody(java.io.InputStream body) throws IOException {
		return new String(body.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
	}

}

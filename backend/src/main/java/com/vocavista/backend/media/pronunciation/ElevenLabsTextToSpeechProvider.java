package com.vocavista.backend.media.pronunciation;

import java.io.ByteArrayOutputStream;
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
class ElevenLabsTextToSpeechProvider implements TextToSpeechProvider {

	private static final String MISSING_API_KEY = "__missing__";

	private final RestClient restClient;
	private final ElevenLabsProperties properties;

	@Autowired
	ElevenLabsTextToSpeechProvider(
			RestClient.Builder restClientBuilder,
			ElevenLabsProperties properties) {
		this(restClientBuilder.baseUrl(properties.getBaseUrl()).build(), properties);
	}

	ElevenLabsTextToSpeechProvider(RestClient restClient, ElevenLabsProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public GeneratedAudio generate(PronunciationScript script) {
		if (!StringUtils.hasText(properties.getApiKey()) || MISSING_API_KEY.equals(properties.getApiKey())) {
			throw new MediaGenerationException("tts_provider_not_configured", "ElevenLabs API key is not configured");
		}

		byte[] firstWord = requestAudio(script.word() + "...", script.language(), properties.getFirstWordSpeed());
		byte[] secondWord = requestAudio(script.word() + "!", script.language(), properties.getSecondWordSpeed());
		byte[] phrase = requestAudio(punctuated(script.phrase()), script.language(), properties.getPhraseSpeed());
		return new GeneratedAudio(join(firstWord, secondWord, phrase), contentTypeFor(properties.getOutputFormat()));
	}

	@Override
	public String providerName() {
		return "elevenlabs";
	}

	@Override
	public String modelName() {
		return "%s:%s:%s:speeds-%s-%s-%s".formatted(properties.getModelId(), properties.getVoiceId(),
				properties.getOutputFormat(), properties.getFirstWordSpeed(), properties.getSecondWordSpeed(),
				properties.getPhraseSpeed());
	}

	private byte[] requestAudio(String text, String language, double speed) {
		byte[] bytes = restClient.post()
				.uri(uriBuilder -> uriBuilder.path("/v1/text-to-speech/{voiceId}")
						.queryParam("output_format", properties.getOutputFormat())
						.build(properties.getVoiceId()))
				.header("xi-api-key", properties.getApiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestBody(text, language, speed))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new MediaGenerationException("tts_provider_error", "ElevenLabs returned HTTP "
							+ response.getStatusCode().value() + ": " + readBody(response.getBody()));
				})
				.body(byte[].class);

		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("tts_provider_error", "ElevenLabs returned empty audio");
		}
		return bytes;
	}

	private Map<String, Object> requestBody(String text, String language, double speed) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", text);
		body.put("model_id", properties.getModelId());
		body.put("language_code", language);
		body.put("voice_settings", Map.of(
				"stability", properties.getStability(),
				"similarity_boost", properties.getSimilarityBoost(),
				"style", properties.getStyle(),
				"speed", speed,
				"use_speaker_boost", properties.isUseSpeakerBoost()));
		return body;
	}

	private static byte[] join(byte[]... parts) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (byte[] part : parts) {
			output.writeBytes(part);
		}
		return output.toByteArray();
	}

	private static String punctuated(String value) {
		return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") ? value : value + ".";
	}

	private static String contentTypeFor(String outputFormat) {
		if (outputFormat.startsWith("wav_")) {
			return "audio/wav";
		}
		return "audio/mpeg";
	}

	private static String readBody(java.io.InputStream body) throws IOException {
		return new String(body.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
	}

}

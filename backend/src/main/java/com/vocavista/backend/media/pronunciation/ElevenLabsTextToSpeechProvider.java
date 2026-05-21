package com.vocavista.backend.media.pronunciation;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${vocavista.media.provider-mode:fake}' == 'real' and '${vocavista.media.tts-provider:fake}' == 'elevenlabs'")
class ElevenLabsTextToSpeechProvider implements TextToSpeechProvider {

	private static final String MISSING_API_KEY = "__missing__";

	private final RestClient restClient;
	private final String apiKey;
	private final String voiceId;
	private final String modelId;
	private final String outputFormat;
	private final double stability;
	private final double similarityBoost;
	private final double style;
	private final double firstWordSpeed;
	private final double secondWordSpeed;
	private final double phraseSpeed;
	private final boolean useSpeakerBoost;

	@Autowired
	ElevenLabsTextToSpeechProvider(
			RestClient.Builder restClientBuilder,
			@Value("${vocavista.media.elevenlabs.base-url}") String baseUrl,
			@Value("${vocavista.media.elevenlabs.api-key}") String apiKey,
			@Value("${vocavista.media.elevenlabs.voice-id}") String voiceId,
			@Value("${vocavista.media.elevenlabs.model-id}") String modelId,
			@Value("${vocavista.media.elevenlabs.output-format}") String outputFormat,
			@Value("${vocavista.media.elevenlabs.stability:0.5}") double stability,
			@Value("${vocavista.media.elevenlabs.similarity-boost:0.75}") double similarityBoost,
			@Value("${vocavista.media.elevenlabs.style:0.0}") double style,
			@Value("${vocavista.media.elevenlabs.first-word-speed:0.72}") double firstWordSpeed,
			@Value("${vocavista.media.elevenlabs.second-word-speed:1.0}") double secondWordSpeed,
			@Value("${vocavista.media.elevenlabs.phrase-speed:0.86}") double phraseSpeed,
			@Value("${vocavista.media.elevenlabs.use-speaker-boost:true}") boolean useSpeakerBoost) {
		this(restClientBuilder.baseUrl(baseUrl).build(), apiKey, voiceId, modelId, outputFormat, stability,
				similarityBoost, style, firstWordSpeed, secondWordSpeed, phraseSpeed, useSpeakerBoost);
	}

	ElevenLabsTextToSpeechProvider(
			RestClient restClient,
			String apiKey,
			String voiceId,
			String modelId,
			String outputFormat,
			double stability,
			double similarityBoost,
			double style,
			double firstWordSpeed,
			double secondWordSpeed,
			double phraseSpeed,
			boolean useSpeakerBoost) {
		this.restClient = restClient;
		this.apiKey = apiKey;
		this.voiceId = voiceId;
		this.modelId = modelId;
		this.outputFormat = outputFormat;
		this.stability = stability;
		this.similarityBoost = similarityBoost;
		this.style = style;
		this.firstWordSpeed = firstWordSpeed;
		this.secondWordSpeed = secondWordSpeed;
		this.phraseSpeed = phraseSpeed;
		this.useSpeakerBoost = useSpeakerBoost;
	}

	@Override
	public GeneratedAudio generate(PronunciationScript script) {
		if (!StringUtils.hasText(apiKey) || MISSING_API_KEY.equals(apiKey)) {
			throw new MediaGenerationException("tts_provider_not_configured", "ElevenLabs API key is not configured");
		}

		byte[] firstWord = requestAudio(script.word() + "...", script.language(), firstWordSpeed);
		byte[] secondWord = requestAudio(script.word() + "!", script.language(), secondWordSpeed);
		byte[] phrase = requestAudio(punctuated(script.phrase()), script.language(), phraseSpeed);
		return new GeneratedAudio(join(firstWord, secondWord, phrase), contentTypeFor(outputFormat));
	}

	@Override
	public String providerName() {
		return "elevenlabs";
	}

	@Override
	public String modelName() {
		return modelId + ":" + voiceId + ":" + outputFormat + ":speeds-" + firstWordSpeed + "-" + secondWordSpeed
				+ "-" + phraseSpeed;
	}

	private byte[] requestAudio(String text, String language, double speed) {
		byte[] bytes = restClient.post()
				.uri(uriBuilder -> uriBuilder.path("/v1/text-to-speech/{voiceId}")
						.queryParam("output_format", outputFormat)
						.build(voiceId))
				.header("xi-api-key", apiKey)
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
		body.put("model_id", modelId);
		body.put("language_code", language);
		body.put("voice_settings", Map.of(
				"stability", stability,
				"similarity_boost", similarityBoost,
				"style", style,
				"speed", speed,
				"use_speaker_boost", useSpeakerBoost));
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

package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

@Component
class PronunciationVideoGenerator {

	private final RestClient restClient;
	private final VeoProperties properties;

	PronunciationVideoGenerator(RestClient.Builder restClientBuilder, VeoProperties properties) {
		this.restClient = restClientBuilder
				.baseUrl(properties.getBaseUrl())
				.defaultHeader("x-goog-api-key", properties.getApiKey())
				.build();
		this.properties = properties;
	}

	GeneratedVideo generate(PronunciationScript script) {
		if (!StringUtils.hasText(properties.getApiKey()) || "__missing__".equals(properties.getApiKey())) {
			throw new MediaGenerationException("video_provider_unavailable", "Google AI API key is not configured");
		}

		try {
			JsonNode startedOperation = startOperation(script);
			String operationName = text(startedOperation, "name");
			if (!StringUtils.hasText(operationName)) {
				throw new MediaGenerationException("video_provider_error", "Veo did not return an operation name");
			}
			JsonNode completedOperation = waitForCompletion(operationName);
			return extractVideo(completedOperation);
		}
		catch (RestClientResponseException ex) {
			throw new MediaGenerationException("video_provider_error",
					"Veo returned HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString(), ex);
		}
		catch (MediaGenerationException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("video_provider_error", "Veo video generation failed", ex);
		}
	}

	String providerName() {
		return "google-veo";
	}

	String modelName() {
		return "%s:%s:%s:%ss:%s".formatted(properties.getModel(), properties.getAspectRatio(),
				properties.getResolution(), properties.getDurationSeconds(), "prompt-v4");
	}

	private JsonNode startOperation(PronunciationScript script) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("sampleCount", properties.getSampleCount());
		parameters.put("durationSeconds", properties.getDurationSeconds());
		parameters.put("aspectRatio", properties.getAspectRatio());
		if (StringUtils.hasText(properties.getResolution())) {
			parameters.put("resolution", properties.getResolution());
		}
		if (StringUtils.hasText(properties.getPersonGeneration())) {
			parameters.put("personGeneration", properties.getPersonGeneration());
		}

		Map<String, Object> request = Map.of(
				"instances", List.of(Map.of("prompt", promptFor(script))),
				"parameters", parameters);

		return restClient.post()
				.uri("/v1beta/models/{model}:predictLongRunning", properties.getModel())
				.body(request)
				.retrieve()
				.body(JsonNode.class);
	}

	private JsonNode waitForCompletion(String operationName) {
		Instant deadline = Instant.now().plus(properties.getTimeout());
		while (Instant.now().isBefore(deadline)) {
			JsonNode operation = restClient.get()
					.uri(operationPath(operationName))
					.retrieve()
					.body(JsonNode.class);
			if (operation != null && operation.path("done").asBoolean(false)) {
				JsonNode error = operation.path("error");
				if (!error.isMissingNode() && !error.isNull()) {
					throw new MediaGenerationException("video_provider_error",
							"Veo operation failed: " + text(error, "message"));
				}
				return operation;
			}
			sleep();
		}
		throw new MediaGenerationException("video_provider_timeout", "Timed out waiting for Veo video generation");
	}

	private GeneratedVideo extractVideo(JsonNode operation) {
		JsonNode video = firstVideo(operation.path("response"));
		if (video.isMissingNode()) {
			throw new MediaGenerationException("video_provider_error", "Veo completed without a generated video");
		}

		String contentType = firstText(video, "mimeType", "mime_type");
		if (!StringUtils.hasText(contentType)) {
			contentType = "video/mp4";
		}

		String base64Bytes = firstText(video, "bytes", "videoBytes", "video_bytes");
		if (StringUtils.hasText(base64Bytes)) {
			return new GeneratedVideo(Base64.getDecoder().decode(base64Bytes), contentType);
		}

		String uri = firstText(video, "uri", "fileUri", "file_uri", "gcsUri", "gcs_uri");
		if (!StringUtils.hasText(uri)) {
			throw new MediaGenerationException("video_provider_error", "Veo generated video did not include bytes or a URI");
		}
		return downloadVideo(URI.create(uri), contentType);
	}

	private GeneratedVideo downloadVideo(URI uri, String fallbackContentType) {
		ResponseEntity<byte[]> response = restClient.get().uri(uri).retrieve().toEntity(byte[].class);
		byte[] bytes = response.getBody();
		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("video_provider_error", "Veo returned empty video");
		}
		String contentType = response.getHeaders().getContentType() == null
				? fallbackContentType
				: response.getHeaders().getContentType().toString();
		return new GeneratedVideo(bytes, contentType);
	}

	private JsonNode firstVideo(JsonNode response) {
		for (String pointer : List.of(
				"/generateVideoResponse/generatedSamples/0/video",
				"/generateVideoResponse/generatedVideos/0/video",
				"/generatedSamples/0/video",
				"/generatedVideos/0/video",
				"/videos/0/video")) {
			JsonNode node = response.at(pointer);
			if (!node.isMissingNode() && !node.isNull()) {
				return node;
			}
		}
		return MissingNode.getInstance();
	}

	private String operationPath(String operationName) {
		if (operationName.startsWith("http://") || operationName.startsWith("https://")) {
			return URI.create(operationName).getRawPath();
		}
		return "/v1beta/" + operationName;
	}

	private void sleep() {
		try {
			Thread.sleep(properties.getPollInterval().toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new MediaGenerationException("video_provider_interrupted", "Interrupted while waiting for Veo video generation", ex);
		}
	}

	private static String promptFor(PronunciationScript script) {
		String spokenText = script.text().replaceAll("\\s+", " ").trim();
		return """
				Create a vertical close-up lip-sync video of one %s.
				The complete audio transcript must be exactly this German script and nothing else: "%s".
				Match mouth movements to the quoted German words.
				Use a simple neutral background.
				""".formatted(script.speakerDescription(), spokenText).replaceAll("\\s+", " ").trim();
	}

	private static String text(JsonNode node, String fieldName) {
		return node == null ? "" : node.path(fieldName).asText("");
	}

	private static String firstText(JsonNode node, String... fieldNames) {
		for (String fieldName : fieldNames) {
			String value = text(node, fieldName);
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return "";
	}

}

record GeneratedVideo(byte[] bytes, String contentType) {
}

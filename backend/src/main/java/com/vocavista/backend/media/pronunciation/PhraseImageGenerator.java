package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.util.ArrayList;
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
class PhraseImageGenerator {

	private final RestClient restClient;
	private final ImagenProperties properties;

	PhraseImageGenerator(RestClient.Builder restClientBuilder, ImagenProperties properties) {
		this.restClient = restClientBuilder
				.baseUrl(properties.getBaseUrl())
				.defaultHeader("x-goog-api-key", properties.getApiKey())
				.build();
		this.properties = properties;
	}

	List<GeneratedImage> generate(PhraseImagePrompt prompt) {
		if (!StringUtils.hasText(properties.getApiKey()) || "__missing__".equals(properties.getApiKey())) {
			throw new MediaGenerationException("image_provider_unavailable", "Google AI API key is not configured");
		}

		try {
			JsonNode response = predict(prompt);
			return extractImages(response);
		}
		catch (RestClientResponseException ex) {
			throw new MediaGenerationException("image_provider_error",
					"Imagen returned HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString(), ex);
		}
		catch (MediaGenerationException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("image_provider_error", "Imagen image generation failed", ex);
		}
	}

	String providerName() {
		return "google-imagen";
	}

	String modelName() {
		return "%s:%s:%s:%s".formatted(properties.getModel(), properties.getAspectRatio(),
				properties.getSampleImageSize(), "prompt-v4");
	}

	private JsonNode predict(PhraseImagePrompt prompt) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("sampleCount", properties.getSampleCount());
		parameters.put("aspectRatio", properties.getAspectRatio());
		if (StringUtils.hasText(properties.getSampleImageSize())) {
			parameters.put("sampleImageSize", properties.getSampleImageSize());
		}

		Map<String, Object> request = Map.of(
				"instances", List.of(Map.of("prompt", prompt.text())),
				"parameters", parameters);

		return restClient.post()
				.uri("/v1beta/models/{model}:predict", properties.getModel())
				.body(request)
				.retrieve()
				.body(JsonNode.class);
	}

	private List<GeneratedImage> extractImages(JsonNode response) {
		List<JsonNode> imageNodes = imageNodes(response);
		if (imageNodes.isEmpty()) {
			throw new MediaGenerationException("image_provider_error", "Imagen completed without a generated image");
		}
		return imageNodes.stream().map(this::extractImage).toList();
	}

	private GeneratedImage extractImage(JsonNode image) {
		String contentType = firstText(image, "mimeType", "mime_type");
		if (!StringUtils.hasText(contentType)) {
			contentType = "image/png";
		}

		String base64Bytes = firstText(image, "bytesBase64Encoded", "bytes_base64_encoded", "bytes", "imageBytes", "image_bytes");
		if (StringUtils.hasText(base64Bytes)) {
			return new GeneratedImage(decodeBase64Image(base64Bytes), contentType);
		}

		String uri = firstText(image, "uri", "fileUri", "file_uri", "gcsUri", "gcs_uri");
		if (!StringUtils.hasText(uri)) {
			throw new MediaGenerationException("image_provider_error", "Imagen generated image did not include bytes or a URI");
		}
		return downloadImage(URI.create(uri), contentType);
	}

	private GeneratedImage downloadImage(URI uri, String fallbackContentType) {
		ResponseEntity<byte[]> response = restClient.get().uri(uri).retrieve().toEntity(byte[].class);
		byte[] bytes = response.getBody();
		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("image_provider_error", "Imagen returned empty image");
		}
		String contentType = response.getHeaders().getContentType() == null
				? fallbackContentType
				: response.getHeaders().getContentType().toString();
		return new GeneratedImage(bytes, contentType);
	}

	private static byte[] decodeBase64Image(String value) {
		int commaIndex = value.indexOf(',');
		String base64 = commaIndex >= 0 ? value.substring(commaIndex + 1) : value;
		return Base64.getDecoder().decode(base64);
	}

	private static List<JsonNode> imageNodes(JsonNode response) {
		for (String pointer : List.of("/predictions", "/generatedImages", "/generated_images", "/images")) {
			JsonNode array = response.at(pointer);
			if (!array.isArray()) {
				continue;
			}
			List<JsonNode> images = new ArrayList<>();
			for (JsonNode item : array) {
				JsonNode image = item.path("image");
				images.add(image.isMissingNode() || image.isNull() ? item : image);
			}
			if (!images.isEmpty()) {
				return images;
			}
		}
		JsonNode singleImage = response.at("/image");
		return singleImage.isMissingNode() || singleImage.isNull() ? List.of() : List.of(singleImage);
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

record PhraseImagePrompt(String word, String phrase, String language, String text, String version) {
}

record GeneratedImage(byte[] bytes, String contentType) {
}

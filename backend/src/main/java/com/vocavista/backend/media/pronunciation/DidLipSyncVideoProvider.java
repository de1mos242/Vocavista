package com.vocavista.backend.media.pronunciation;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${vocavista.media.provider-mode:fake}' == 'real' and '${vocavista.media.lipsync-provider:fake}' == 'did'")
class DidLipSyncVideoProvider implements LipSyncVideoProvider {

	private static final String MISSING_API_KEY = "__missing__";

	private final RestClient restClient;
	private final RestClient downloadClient;
	private final String apiKey;
	private final String sourceUrl;
	private final long timeoutSeconds;
	private final long pollIntervalMillis;
	private final Clock clock;

	@Autowired
	DidLipSyncVideoProvider(
			RestClient.Builder restClientBuilder,
			@Value("${vocavista.media.did.base-url}") String baseUrl,
			@Value("${vocavista.media.did.api-key}") String apiKey,
			@Value("${vocavista.media.did.source-url}") String sourceUrl,
			@Value("${vocavista.media.did.timeout-seconds:120}") long timeoutSeconds,
			@Value("${vocavista.media.did.poll-interval-millis:1000}") long pollIntervalMillis) {
		this(restClientBuilder.baseUrl(baseUrl).build(), RestClient.create(), apiKey, sourceUrl, timeoutSeconds,
				pollIntervalMillis, Clock.systemUTC());
	}

	DidLipSyncVideoProvider(
			RestClient restClient,
			RestClient downloadClient,
			String apiKey,
			String sourceUrl,
			long timeoutSeconds,
			long pollIntervalMillis,
			Clock clock) {
		this.restClient = restClient;
		this.downloadClient = downloadClient;
		this.apiKey = apiKey;
		this.sourceUrl = sourceUrl;
		this.timeoutSeconds = timeoutSeconds;
		this.pollIntervalMillis = pollIntervalMillis;
		this.clock = clock;
	}

	@Override
	public GeneratedVideo generate(PronunciationScript script, GeneratedAudio audio, URI audioUrl) {
		if (!StringUtils.hasText(apiKey) || MISSING_API_KEY.equals(apiKey)) {
			throw new MediaGenerationException("lipsync_provider_not_configured", "D-ID API key is not configured");
		}
		if (!StringUtils.hasText(sourceUrl)) {
			throw new MediaGenerationException("lipsync_provider_not_configured", "D-ID source image URL is not configured");
		}

		String talkId = createTalk(script, audioUrl);
		URI resultUrl = waitForResult(talkId);
		byte[] bytes = downloadClient.get()
				.uri(resultUrl)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new MediaGenerationException("lipsync_provider_error", "D-ID result download returned HTTP "
							+ response.getStatusCode().value());
				})
				.body(byte[].class);

		if (bytes == null || bytes.length == 0) {
			throw new MediaGenerationException("lipsync_provider_error", "D-ID returned empty video");
		}
		return new GeneratedVideo(bytes, "video/mp4");
	}

	@Override
	public String providerName() {
		return "did";
	}

	@Override
	public String modelName() {
		return "talks:" + sourceUrl;
	}

	private String createTalk(PronunciationScript script, URI audioUrl) {
		Map<String, Object> response = restClient.post()
				.uri("/talks")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.body(createTalkBody(script, audioUrl))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
					throw new MediaGenerationException("lipsync_provider_error", "D-ID create talk returned HTTP "
							+ errorResponse.getStatusCode().value() + ": " + readBody(errorResponse.getBody()));
				})
				.body(new ParameterizedTypeReference<>() {
				});

		Object id = response == null ? null : response.get("id");
		if (!(id instanceof String talkId) || !StringUtils.hasText(talkId)) {
			throw new MediaGenerationException("lipsync_provider_error", "D-ID create talk response did not include an id");
		}
		return talkId;
	}

	private URI waitForResult(String talkId) {
		Instant deadline = clock.instant().plusSeconds(timeoutSeconds);
		while (clock.instant().isBefore(deadline)) {
			Map<String, Object> response = getTalk(talkId);
			Object status = response.get("status");
			if ("done".equals(status)) {
				Object resultUrl = response.get("result_url");
				if (resultUrl instanceof String url && StringUtils.hasText(url)) {
					return URI.create(url);
				}
				throw new MediaGenerationException("lipsync_provider_error", "D-ID completed without a result URL");
			}
			if ("error".equals(status) || "rejected".equals(status)) {
				throw new MediaGenerationException("lipsync_provider_error", "D-ID talk failed with status " + status);
			}
			sleep();
		}
		throw new MediaGenerationException("lipsync_provider_timeout", "D-ID talk did not complete before timeout");
	}

	private Map<String, Object> getTalk(String talkId) {
		return restClient.get()
				.uri("/talks/{id}", talkId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new MediaGenerationException("lipsync_provider_error", "D-ID get talk returned HTTP "
							+ response.getStatusCode().value() + ": " + readBody(response.getBody()));
				})
				.body(new ParameterizedTypeReference<>() {
				});
	}

	private Map<String, Object> createTalkBody(PronunciationScript script, URI audioUrl) {
		Map<String, Object> scriptBody = new LinkedHashMap<>();
		scriptBody.put("type", "audio");
		scriptBody.put("audio_url", audioUrl.toString());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("source_url", sourceUrl);
		body.put("script", scriptBody);
		body.put("name", "vocavista-pronunciation-" + script.word());
		return body;
	}

	private String authorizationHeader() {
		return apiKey.startsWith("Basic ") ? apiKey : "Basic " + apiKey;
	}

	private void sleep() {
		try {
			Thread.sleep(pollIntervalMillis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new MediaGenerationException("lipsync_provider_interrupted", "Interrupted while waiting for D-ID", ex);
		}
	}

	private static String readBody(java.io.InputStream body) throws IOException {
		return new String(body.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
	}

}

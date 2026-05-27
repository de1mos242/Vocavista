package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PronunciationVideoGeneratorTest {

	@Test
	void createsVeoVideoAndDownloadsCompletedOperationResult() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		VeoProperties properties = properties();
		PronunciationVideoGenerator generator = new PronunciationVideoGenerator(restClientBuilder, properties);

		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-lite-generate-preview:predictLongRunning"))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andExpect(content().string(containsString("\"aspectRatio\":\"9:16\"")))
				.andExpect(content().string(containsString("\"durationSeconds\":6")))
				.andExpect(content().string(not(containsString("personGeneration"))))
				.andExpect(content().string(containsString("female adult speaker")))
				.andExpect(content().string(containsString("Start with 0.5 seconds of absolute silence")))
				.andExpect(content().string(containsString("The first audible speech must be the first word of the quoted German script")))
				.andExpect(content().string(containsString("do not add any greeting, intro, explanation, filler, English, Russian, or invented words")))
				.andRespond(withSuccess("""
						{"name":"models/veo-3.1-lite-generate-preview/operations/abc"}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-lite-generate-preview/operations/abc"))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andRespond(withSuccess("""
						{"done":true,"response":{"generateVideoResponse":{"generatedSamples":[{"video":{"uri":"https://files.example/video.mp4","mimeType":"video/mp4"}}]}}}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://files.example/video.mp4"))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andRespond(withSuccess("video".getBytes(), MediaType.parseMediaType("video/mp4")));

		GeneratedVideo video = generator.generate(script());

		assertThat(video.bytes()).isEqualTo("video".getBytes());
		assertThat(video.contentType()).isEqualTo("video/mp4");
		assertThat(generator.providerName()).isEqualTo("google-veo");
		assertThat(generator.modelName()).contains("veo-3.1-lite-generate-preview", "9:16", "6s", "prompt-v4");
		server.verify();
	}

	@Test
	void rejectsMissingApiKeyWithoutCallingProvider() {
		VeoProperties properties = properties();
		properties.setApiKey("__missing__");
		PronunciationVideoGenerator generator = new PronunciationVideoGenerator(RestClient.builder(), properties);

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("Google AI API key is not configured");
	}

	private static VeoProperties properties() {
		VeoProperties properties = new VeoProperties();
		properties.setApiKey("test-key");
		properties.setPollInterval(Duration.ZERO);
		properties.setTimeout(Duration.ofSeconds(1));
		return properties;
	}

	private static PronunciationScript script() {
		return new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de",
				"Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v6", "female adult speaker");
	}

}

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
				.andExpect(content().string(containsString("\"resolution\":\"720p\"")))
				.andExpect(content().string(containsString("\"durationSeconds\":4")))
				.andExpect(content().string(not(containsString("personGeneration"))))
				.andExpect(content().string(containsString("female german adult speaker")))
				.andExpect(content().string(containsString("Close-up talking head")))
				.andExpect(content().string(containsString("says exactly")))
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
		assertThat(generator.modelName()).contains("veo-3.1-lite-generate-preview", "9:16", "720p", "4-6s", "prompt-v6");
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

	@Test
	void requestsFourSecondVideoForSevenWordPhrase() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		VeoProperties properties = properties();
		PronunciationVideoGenerator generator = new PronunciationVideoGenerator(restClientBuilder, properties);

		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-lite-generate-preview:predictLongRunning"))
				.andExpect(content().string(containsString("\"durationSeconds\":4")))
				.andExpect(content().string(containsString("Bitte erklären Sie mir den genauen Vorgang.")))
				.andRespond(withSuccess("""
						{"name":"models/veo-3.1-lite-generate-preview/operations/abc"}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-lite-generate-preview/operations/abc"))
				.andRespond(withSuccess("""
						{"done":true,"response":{"generateVideoResponse":{"generatedSamples":[{"video":{"bytes":"dmlkZW8=","mimeType":"video/mp4"}}]}}}
						""", MediaType.APPLICATION_JSON));

		generator.generate(new PronunciationScript("Vorgang", "Bitte erklären Sie mir den genauen Vorgang.", "de",
				"Vorgang...\n\nder Vorgang!\n\nBitte erklären Sie mir den genauen Vorgang.", "v6",
				"male german adult speaker"));

		server.verify();
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
				"Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v6", "female german adult speaker");
	}

}

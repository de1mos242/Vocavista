package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PronunciationAudioGeneratorTest {

	@Test
	void createsSpeechRequestWithConfiguredVoiceModelAndInstructions() {
		OpenAiTextToSpeechProperties properties = properties();
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(properties, restClientBuilder);

		server.expect(once(), requestTo("https://api.openai.com/v1/audio/speech"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer api-key"))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.model").value("gpt-4o-mini-tts"))
				.andExpect(jsonPath("$.voice").value("coral"))
				.andExpect(jsonPath("$.input").value("Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe."))
				.andExpect(jsonPath("$.instructions").value("Speak clearly in German."))
				.andExpect(jsonPath("$.response_format").value("mp3"))
				.andRespond(withSuccess("audio".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

		GeneratedAudio audio = generator.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german"));

		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.bytes()).isEqualTo("audio".getBytes());
		assertThat(generator.providerName()).isEqualTo("openai");
		assertThat(generator.modelName()).contains("gpt-4o-mini-tts", "coral", "mp3", "Speak clearly in German.");
		server.verify();
	}

	@Test
	void failsWhenApiKeyIsMissing() {
		OpenAiTextToSpeechProperties properties = properties();
		properties.setApiKey("__missing__");
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(properties, RestClient.builder());

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI API key is not configured");
	}

	@Test
	void mapsProviderErrors() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(properties(), restClientBuilder);

		server.expect(once(), requestTo("https://api.openai.com/v1/audio/speech"))
				.andRespond(withBadRequest().body("bad request"));

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessageContaining("OpenAI returned HTTP 400");
		server.verify();
	}

	@Test
	void rejectsEmptyAudio() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(properties(), restClientBuilder);

		server.expect(once(), requestTo("https://api.openai.com/v1/audio/speech"))
				.andRespond(withSuccess(new byte[0], MediaType.APPLICATION_OCTET_STREAM));

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI returned empty audio");
		server.verify();
	}

	private static OpenAiTextToSpeechProperties properties() {
		OpenAiTextToSpeechProperties properties = new OpenAiTextToSpeechProperties();
		properties.setApiKey("api-key");
		properties.setInstructions("Speak clearly in German.");
		return properties;
	}

	private static PronunciationScript script() {
		return new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de",
				"Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german");
	}

}

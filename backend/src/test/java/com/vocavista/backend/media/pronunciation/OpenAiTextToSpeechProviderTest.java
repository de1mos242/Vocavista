package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiTextToSpeechProviderTest {

	@Test
	void createsSpeechWithConfiguredVoiceModelAndInstructions() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiTextToSpeechProperties properties = properties();
		OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(builder.build(), properties);

		server.expect(requestTo("https://api.openai.test/v1/audio/speech"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer api-key"))
				.andExpect(content().json("""
						{
						  "model": "gpt-4o-mini-tts",
						  "voice": "coral",
						  "input": "Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.",
						  "instructions": "Speak clearly in German.",
						  "response_format": "mp3"
						}
						"""))
				.andRespond(withSuccess("audio".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

		GeneratedAudio audio = provider.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german"));

		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.bytes()).isEqualTo("audio".getBytes());
		assertThat(provider.providerName()).isEqualTo("openai");
		assertThat(provider.modelName()).contains("gpt-4o-mini-tts", "coral", "mp3", "Speak clearly in German.");
		server.verify();
	}

	@Test
	void failsWhenApiKeyIsMissing() {
		OpenAiTextToSpeechProperties properties = properties();
		properties.setApiKey("__missing__");
		OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(RestClient.create(), properties);

		assertThatThrownBy(() -> provider.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI API key is not configured");
	}

	@Test
	void mapsProviderErrors() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(builder.build(), properties());

		server.expect(requestTo("https://api.openai.test/v1/audio/speech"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad request"));

		assertThatThrownBy(() -> provider.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessageContaining("OpenAI returned HTTP 400: bad request");
		server.verify();
	}

	@Test
	void rejectsEmptyAudio() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(builder.build(), properties());

		server.expect(requestTo("https://api.openai.test/v1/audio/speech"))
				.andRespond(withSuccess(new byte[0], MediaType.APPLICATION_OCTET_STREAM));

		assertThatThrownBy(() -> provider.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI returned empty audio");
		server.verify();
	}

	private static OpenAiTextToSpeechProperties properties() {
		OpenAiTextToSpeechProperties properties = new OpenAiTextToSpeechProperties();
		properties.setBaseUrl("https://api.openai.test");
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

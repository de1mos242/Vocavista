package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PhraseImageGeneratorTest {

	@Test
	void createsCinematicImagenImage() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		ImagenProperties properties = properties();
		PhraseImageGenerator generator = new PhraseImageGenerator(restClientBuilder, properties);

		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/imagen-4.0-generate-001:predict"))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andExpect(content().string(containsString("\"aspectRatio\":\"16:9\"")))
				.andExpect(content().string(containsString("\"sampleImageSize\":\"1K\"")))
				.andExpect(content().string(containsString("Full phrase context")))
				.andExpect(content().string(containsString("Target word")))
				.andRespond(withSuccess("""
						{"predictions":[{"bytesBase64Encoded":"%s","mimeType":"image/png"}]}
						""".formatted(Base64.getEncoder().encodeToString("image".getBytes())), MediaType.APPLICATION_JSON));

		GeneratedImage image = generator.generate(prompt());

		assertThat(image.bytes()).isEqualTo("image".getBytes());
		assertThat(image.contentType()).isEqualTo("image/png");
		assertThat(generator.providerName()).isEqualTo("google-imagen");
		assertThat(generator.modelName()).contains("imagen-4.0-generate-001", "16:9", "1K", "prompt-v1");
		server.verify();
	}

	@Test
	void rejectsMissingApiKeyWithoutCallingProvider() {
		ImagenProperties properties = properties();
		properties.setApiKey("__missing__");
		PhraseImageGenerator generator = new PhraseImageGenerator(RestClient.builder(), properties);

		assertThatThrownBy(() -> generator.generate(prompt()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("Google AI API key is not configured");
	}

	private static ImagenProperties properties() {
		ImagenProperties properties = new ImagenProperties();
		properties.setApiKey("test-key");
		return properties;
	}

	private static PhraseImagePrompt prompt() {
		return new PhraseImagePrompt("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de",
				"Full phrase context: Ich mache meine Hausaufgabe. Target word: Hausaufgabe.", "v1");
	}

}

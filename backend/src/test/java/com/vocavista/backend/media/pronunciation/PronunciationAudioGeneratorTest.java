package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openai.core.JsonValue;
import com.openai.core.http.Headers;
import com.openai.core.http.HttpResponse;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.audio.speech.SpeechCreateParams;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PronunciationAudioGeneratorTest {

	@Test
	void createsSpeechWithConfiguredVoiceModelAndInstructions() {
		OpenAiTextToSpeechProperties properties = properties();
		CapturingSpeechGenerator speechGenerator = new CapturingSpeechGenerator("audio".getBytes());
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(speechGenerator, properties);

		GeneratedAudio audio = generator.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german"));

		SpeechCreateParams request = speechGenerator.request;
		assertThat(request.model().asString()).isEqualTo("gpt-4o-mini-tts");
		assertThat(request.voice().asString()).isEqualTo("coral");
		assertThat(request.input()).isEqualTo("Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.");
		assertThat(request.instructions()).contains("Speak clearly in German.");
		assertThat(request.responseFormat().map(SpeechCreateParams.ResponseFormat::asString)).contains("mp3");
		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.bytes()).isEqualTo("audio".getBytes());
		assertThat(generator.providerName()).isEqualTo("openai");
		assertThat(generator.modelName()).contains("gpt-4o-mini-tts", "coral", "mp3", "Speak clearly in German.");
	}

	@Test
	void failsWhenApiKeyIsMissing() {
		OpenAiTextToSpeechProperties properties = properties();
		properties.setApiKey("__missing__");
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(params -> new TestHttpResponse("audio".getBytes()),
				properties);

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI API key is not configured");
	}

	@Test
	void mapsProviderErrors() {
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(params -> {
			throw new TestOpenAIServiceException(400, "bad request");
		}, properties());

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessageContaining("OpenAI returned HTTP 400");
	}

	@Test
	void rejectsEmptyAudio() {
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(params -> new TestHttpResponse(new byte[0]),
				properties());

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI returned empty audio");
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

	private static final class CapturingSpeechGenerator implements Function<SpeechCreateParams, HttpResponse> {

		private final byte[] response;
		private SpeechCreateParams request;

		private CapturingSpeechGenerator(byte[] response) {
			this.response = response;
		}

		@Override
		public HttpResponse apply(SpeechCreateParams params) {
			request = params;
			return new TestHttpResponse(response);
		}

	}

	private record TestHttpResponse(byte[] bytes) implements HttpResponse {

		@Override
		public int statusCode() {
			return 200;
		}

		@Override
		public Headers headers() {
			return Headers.builder().build();
		}

		@Override
		public InputStream body() {
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public void close() {
		}

	}

	private static final class TestOpenAIServiceException extends OpenAIServiceException {

		private final int statusCode;
		private final JsonValue body;

		private TestOpenAIServiceException(int statusCode, String body) {
			super(body, null);
			this.statusCode = statusCode;
			this.body = JsonValue.from(body);
		}

		@Override
		public int statusCode() {
			return statusCode;
		}

		@Override
		public Headers headers() {
			return Headers.builder().build();
		}

		@Override
		public JsonValue body() {
			return body;
		}

		@Override
		public Optional<String> code() {
			return Optional.empty();
		}

		@Override
		public Optional<String> param() {
			return Optional.empty();
		}

		@Override
		public Optional<String> type() {
			return Optional.empty();
		}

	}

}

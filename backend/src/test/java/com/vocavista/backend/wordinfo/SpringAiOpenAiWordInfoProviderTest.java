package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.http.Headers;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ErrorObject;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.mockito.ArgumentCaptor;

class SpringAiOpenAiWordInfoProviderTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void parsesStructuredProviderResponse() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		ProviderWordInfo wordInfo = provider.generate("Hausaufgabe");

		assertThat(wordInfo.normalizedWord()).isEqualTo("Hausaufgabe");
		assertThat(wordInfo.examples()).hasSize(3);
		assertThat(wordInfo.translations().en()).contains("homework");
	}

	@Test
	void sendsLocalizedTextSchemaWithExplicitLanguageProperties() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		provider.generate("Hausaufgabe");

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(promptCaptor.capture());
		OpenAiChatOptions options = (OpenAiChatOptions) promptCaptor.getValue().getOptions();
		JsonNode responseSchema = OBJECT_MAPPER.readTree(options.getResponseFormat().getJsonSchema());
		JsonNode localizedTextSchema = responseSchema.path("$defs").path("LocalizedText");

		assertThat(localizedTextSchema.path("type").asText()).isEqualTo("object");
		assertThat(localizedTextSchema.path("properties").has("en")).isTrue();
		assertThat(localizedTextSchema.path("properties").has("ru")).isTrue();
		assertThat(localizedTextSchema.path("properties").path("en").path("items").path("type").asText())
				.isEqualTo("string");
		assertThat(localizedTextSchema.path("properties").path("ru").path("items").path("type").asText())
				.isEqualTo("string");
		assertThat(StreamSupport.stream(localizedTextSchema.path("required").spliterator(), false)
				.map(JsonNode::asText)
				.toList()).containsExactly("en", "ru");
		assertThat(localizedTextSchema.path("additionalProperties").asBoolean()).isFalse();
		assertThat(enumValues(responseSchema.path("properties").path("frequency")))
				.containsExactly("rare", "uncommon", "common", "very_common");
		assertThat(enumValues(responseSchema.path("properties").path("partOfSpeech")))
				.containsExactly("noun", "verb", "adjective", "adverb", "pronoun", "preposition", "conjunction",
						"interjection", "phrase", "other");
		assertThat(enumValues(responseSchema.path("properties").path("gender")))
				.containsExactly("masculine", "feminine", "neuter");
		assertThat(hasNullableAnyOf(responseSchema.path("properties").path("gender"))).isTrue();
		assertThat(enumValues(responseSchema.path("properties").path("article"))).containsExactly("der", "die", "das");
		assertThat(hasNullableAnyOf(responseSchema.path("properties").path("article"))).isTrue();
		assertThat(StreamSupport.stream(responseSchema.path("properties").path("plural").path("type").spliterator(), false)
				.map(JsonNode::asText)
				.toList()).containsExactly("string", "null");
	}

	@Test
	void mapsMalformedProviderJsonToBadGateway() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("{not-json"));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		assertThatThrownBy(() -> provider.generate("Hausaufgabe")).isInstanceOf(AiProviderBadGatewayException.class);
	}

	@Test
	void mapsOpenAiHttpErrorsToBadGateway() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenThrow(NotFoundException.builder()
				.headers(Headers.builder().build())
				.error(ErrorObject.builder()
						.message("not found")
						.type("invalid_request_error")
						.code("not_found")
						.param("model")
						.build())
				.build());
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		assertThatThrownBy(() -> provider.generate("Hausaufgabe"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessage("AI provider returned an unsuccessful response");
	}

	@Test
	void mapsOpenAiIoErrorsToUnavailable() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenThrow(new OpenAIIoException("network error"));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		assertThatThrownBy(() -> provider.generate("Hausaufgabe"))
				.isInstanceOf(AiProviderUnavailableException.class)
				.hasMessage("AI provider is unavailable");
	}

	@Test
	void missingApiKeyIsUnavailableWithoutCallingProvider() {
		ChatModel chatModel = mock(ChatModel.class);
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "__missing__", "gpt-4o-mini");

		assertThatThrownBy(() -> provider.generate("Hausaufgabe")).isInstanceOf(AiProviderUnavailableException.class);
		verifyNoInteractions(chatModel);
	}

	private static ChatResponse chatResponse(String content) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
	}

	private static List<String> enumValues(JsonNode propertySchema) {
		JsonNode enumNode = propertySchema.has("enum") ? propertySchema.path("enum")
				: StreamSupport.stream(propertySchema.path("anyOf").spliterator(), false)
						.filter(value -> value.has("enum"))
						.findFirst()
						.map(value -> value.path("enum"))
						.orElse(propertySchema.path("enum"));
		return StreamSupport.stream(enumNode.spliterator(), false)
				.filter(value -> !value.isNull())
				.map(JsonNode::asText)
				.toList();
	}

	private static boolean hasNullableAnyOf(JsonNode propertySchema) {
		return StreamSupport.stream(propertySchema.path("anyOf").spliterator(), false)
				.anyMatch(value -> "null".equals(value.path("type").asText()));
	}

}

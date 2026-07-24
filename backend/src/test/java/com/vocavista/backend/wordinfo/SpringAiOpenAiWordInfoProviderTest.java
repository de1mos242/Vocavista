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
	private static final String WORDS_MODEL = "gpt-5.4-mini";

	@Test
	void parsesStructuredProviderResponse() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		AiWordInfoResult result = provider.generate("Hausaufgabe");
		ProviderWordInfo wordInfo = result.wordInfo();

		assertThat(wordInfo.inputLanguage()).isEqualTo(ProviderWordInfo.InputLanguage.de);
		assertThat(wordInfo.meanings()).hasSize(1);
		assertThat(wordInfo.meanings().getFirst().normalizedWord()).isEqualTo("Hausaufgabe");
		assertThat(wordInfo.meanings().getFirst().examples()).hasSize(3);
		assertThat(wordInfo.meanings().getFirst().translations().en()).contains("homework");
		assertThat(wordInfo.meanings().getFirst().gloss().en()).contains("school work assigned to a student");
		assertThat(result.rawResponse()).isEqualTo(SampleWordInfos.nounInfoJson());
	}

	@Test
	void sendsLocalizedTextSchemaWithExplicitLanguageProperties() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		provider.generate("Hausaufgabe");

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(promptCaptor.capture());
		OpenAiChatOptions options = (OpenAiChatOptions) promptCaptor.getValue().getOptions();
		assertThat(options.getModel()).isEqualTo(WORDS_MODEL);
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
		JsonNode meaningSchema = resolveInlineOrRef(responseSchema,
				responseSchema.path("properties").path("meanings").path("items"));
		assertThat(enumValues(responseSchema.path("properties").path("inputLanguage")))
				.containsExactly("en", "ru", "de");
		assertThat(meaningSchema.path("type").asText()).isEqualTo("object");
		assertThat(meaningSchema.path("required").toString()).contains("gloss");
		assertThat(enumValues(meaningSchema.path("properties").path("frequency")))
				.containsExactly("rare", "uncommon", "common", "very_common");
		assertThat(enumValues(meaningSchema.path("properties").path("partOfSpeech")))
				.containsExactly("noun", "verb", "adjective", "adverb", "pronoun", "preposition", "conjunction",
						"interjection", "phrase", "other");
		assertThat(enumValues(meaningSchema.path("properties").path("gender")))
				.containsExactly("masculine", "feminine", "neuter");
		assertThat(hasNullableAnyOf(meaningSchema.path("properties").path("gender"))).isTrue();
		assertThat(enumValues(meaningSchema.path("properties").path("article"))).containsExactly("der", "die", "das");
		assertThat(hasNullableAnyOf(meaningSchema.path("properties").path("article"))).isTrue();
		assertThat(StreamSupport.stream(meaningSchema.path("properties").path("plural").path("type").spliterator(), false)
				.map(JsonNode::asText)
				.toList()).containsExactly("string", "null");
	}

	@Test
	void promptRequiresExamplesToStayTiedToGermanMeaning() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		provider.generate("apple");

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue().getInstructions())
				.anySatisfy(message -> assertThat(message.getText())
						.contains("Every example sentence for a meaning candidate must illustrate only that candidate's meaning")
						.contains("exactly one atomic German sense")
						.contains("mutually distinct senses")
						.contains("never copy the source-language input into German example sentences"));
	}

	@Test
	void mapsMalformedProviderJsonToBadGateway() {
		ChatModel chatModel = mock(ChatModel.class);
		String rawResponse = "{not-json";
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(rawResponse));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		assertThatThrownBy(() -> provider.generate("Hausaufgabe"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("rawProviderResponse=" + rawResponse)
				.extracting(ex -> ((AiProviderBadGatewayException) ex).providerResponse())
				.isEqualTo(rawResponse);
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
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		assertThatThrownBy(() -> provider.generate("Hausaufgabe"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessage("AI provider returned an unsuccessful response");
	}

	@Test
	void mapsOpenAiIoErrorsToUnavailable() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenThrow(new OpenAIIoException("network error"));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", WORDS_MODEL);

		assertThatThrownBy(() -> provider.generate("Hausaufgabe"))
				.isInstanceOf(AiProviderUnavailableException.class)
				.hasMessage("AI provider is unavailable");
	}

	@Test
	void missingApiKeyIsUnavailableWithoutCallingProvider() {
		ChatModel chatModel = mock(ChatModel.class);
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "__missing__", WORDS_MODEL);

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

	private static JsonNode resolveInlineOrRef(JsonNode schema, JsonNode value) {
		String ref = value.path("$ref").asText();
		return ref.isBlank() ? value : schema.at(ref.substring(1));
	}

	private static boolean hasNullableAnyOf(JsonNode propertySchema) {
		return StreamSupport.stream(propertySchema.path("anyOf").spliterator(), false)
				.anyMatch(value -> "null".equals(value.path("type").asText()));
	}

}

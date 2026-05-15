package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.openai.core.http.Headers;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ErrorObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class SpringAiOpenAiWordInfoProviderTest {

	@Test
	void parsesStructuredProviderResponse() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(SampleWordInfos.nounInfoJson()));
		SpringAiOpenAiWordInfoProvider provider = new SpringAiOpenAiWordInfoProvider(chatModel, "test-key", "gpt-4o-mini");

		ProviderWordInfo wordInfo = provider.generate("Hausaufgabe");

		assertThat(wordInfo.normalizedWord()).isEqualTo("Hausaufgabe");
		assertThat(wordInfo.examples()).hasSize(3);
		assertThat(wordInfo.translations()).containsKey("en");
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
				.error(ErrorObject.builder().message("not found").type("invalid_request_error").code("not_found").build())
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

}

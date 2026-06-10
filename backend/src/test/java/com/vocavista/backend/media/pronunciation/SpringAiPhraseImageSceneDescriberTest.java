package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

class SpringAiPhraseImageSceneDescriberTest {

	private static final String WORDS_MODEL = "gpt-5.4-mini";

	@Test
	void generatesVisualSceneDescriptionWithWordsModel() {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("\"A focused student sits at a desk with books.\""));
		SpringAiPhraseImageSceneDescriber describer = new SpringAiPhraseImageSceneDescriber(chatModel, "test-key",
				WORDS_MODEL);

		String description = describer.describe("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de");

		assertThat(description).isEqualTo("A focused student sits at a desk with books.");
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(promptCaptor.capture());
		OpenAiChatOptions options = (OpenAiChatOptions) promptCaptor.getValue().getOptions();
		assertThat(options.getModel()).isEqualTo(WORDS_MODEL);
		assertThat(promptCaptor.getValue().getInstructions())
				.anySatisfy(message -> assertThat(message.getText()).contains("Hausaufgabe",
						"Ich mache meine Hausaufgabe."));
	}

	@Test
	void rejectsMissingApiKeyWithoutCallingProvider() {
		ChatModel chatModel = mock(ChatModel.class);
		SpringAiPhraseImageSceneDescriber describer = new SpringAiPhraseImageSceneDescriber(chatModel, "__missing__",
				WORDS_MODEL);

		assertThatThrownBy(() -> describer.describe("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de"))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI API key is not configured");
		verifyNoInteractions(chatModel);
	}

	private static ChatResponse chatResponse(String content) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
	}

}

package com.vocavista.backend.media.pronunciation;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class SpringAiPhraseImageSceneDescriber {

	private static final String MISSING_API_KEY = "__missing__";

	private static final String SYSTEM_PROMPT = """
			You write concise visual scene descriptions for an image model.
			The input is a German vocabulary word and an example German sentence.
			Return one English sentence, 12 to 30 words, describing concrete physical objects, people, actions, setting, mood, and composition.
			Use only visual details that can be drawn. Keep the description free of German words, quotes, labels, typography, signage, UI, captions, watermarks, and instructions.
			""";

	private final ChatModel chatModel;
	private final String apiKey;
	private final String model;

	SpringAiPhraseImageSceneDescriber(
			ChatModel chatModel,
			@Value("${spring.ai.openai.api-key:__missing__}") String apiKey,
			@Value("${spring.ai.openai.chat.model:gpt-5.4-mini}") String model) {
		this.chatModel = chatModel;
		this.apiKey = apiKey;
		this.model = model;
	}

	public String describe(String word, String phrase, String language) {
		if (!StringUtils.hasText(apiKey) || MISSING_API_KEY.equals(apiKey)) {
			throw new MediaGenerationException("scene_provider_unavailable", "OpenAI API key is not configured");
		}

		try {
			ChatResponse response = chatModel.call(promptFor(word, phrase, language));
			String description = cleanup(response.getResult().getOutput().getText());
			if (!StringUtils.hasText(description)) {
				throw new MediaGenerationException("scene_provider_error", "OpenAI returned an empty scene description");
			}
			return description;
		}
		catch (TransientAiException | OpenAIIoException ex) {
			throw new MediaGenerationException("scene_provider_unavailable", "OpenAI scene description is unavailable", ex);
		}
		catch (NonTransientAiException ex) {
			throw new MediaGenerationException("scene_provider_error", "OpenAI scene description failed", ex);
		}
		catch (OpenAIServiceException ex) {
			if (isTemporarilyUnavailable(ex)) {
				throw new MediaGenerationException("scene_provider_unavailable", "OpenAI scene description is unavailable", ex);
			}
			throw new MediaGenerationException("scene_provider_error", "OpenAI scene description failed", ex);
		}
		catch (MediaGenerationException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("scene_provider_error", "OpenAI scene description failed", ex);
		}
	}

	private Prompt promptFor(String word, String phrase, String language) {
		OpenAiChatOptions options = OpenAiChatOptions.builder()
				.model(model)
				.build();

		return new Prompt(List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userPrompt(word, phrase, language))),
				options);
	}

	private static String userPrompt(String word, String phrase, String language) {
		return "Language: %s\nVocabulary word: %s\nExample sentence: %s".formatted(language, word, phrase);
	}

	private static String cleanup(String description) {
		if (!StringUtils.hasText(description)) {
			return "";
		}
		return description.replaceAll("[`\"']", "")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private static boolean isTemporarilyUnavailable(OpenAIServiceException ex) {
		return ex.statusCode() == 429 || ex.statusCode() >= 500;
	}

}

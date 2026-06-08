package com.vocavista.backend.wordinfo;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class SpringAiOpenAiWordInfoProvider implements AiWordInfoProvider {

	private static final String MISSING_API_KEY = "__missing__";

	private static final String SYSTEM_PROMPT = """
			You generate compact vocabulary metadata for a language-learning app.
			The learning language is German. The learner knows English and Russian.
			Return only JSON matching the provided schema. Do not include markdown.
			Keep translations and notes concise. Do not invent rare meanings unless clearly relevant.
			Use null for noun-only fields when the word is not a noun.
			For German nouns, include gender, article, and plural.
			Use exact enum values: language de; partOfSpeech noun, verb, adjective, adverb, pronoun, preposition, conjunction, interjection, phrase, other; frequency rare, uncommon, common, very_common; gender masculine, feminine, neuter; article der, die, das.
			Examples must contain exactly 3 natural German sentences.
			Example objects must contain only the German sentence and translations.
			Translations, notes, example translations, and compound-part meanings must include both en and ru keys.
			If the word is not a compound, isCompound must be false and compoundParts must be an empty array.
			""";

	private final ChatModel chatModel;
	private final String apiKey;
	private final String model;
	private final BeanOutputConverter<ProviderWordInfo> outputConverter;

	SpringAiOpenAiWordInfoProvider(
			ChatModel chatModel,
			@Value("${spring.ai.openai.api-key:__missing__}") String apiKey,
			@Value("${spring.ai.openai.chat.model:gpt-5.4-nano}") String model) {
		this.chatModel = chatModel;
		this.apiKey = apiKey;
		this.model = model;
		this.outputConverter = new BeanOutputConverter<>(ProviderWordInfo.class);
	}

	@Override
	public AiWordInfoResult generate(String word) {
		if (!StringUtils.hasText(apiKey) || MISSING_API_KEY.equals(apiKey)) {
			throw new AiProviderUnavailableException("AI provider is not configured");
		}

		String content = null;
		try {
			ChatResponse response = chatModel.call(promptFor(word));
			content = response.getResult().getOutput().getText();
			if (!StringUtils.hasText(content)) {
				throw new AiProviderBadGatewayException("AI provider returned empty content");
			}
			return new AiWordInfoResult(outputConverter.convert(content), content);
		}
		catch (TransientAiException ex) {
			throw new AiProviderUnavailableException("AI provider is unavailable", ex);
		}
		catch (NonTransientAiException ex) {
			throw new AiProviderBadGatewayException("AI provider returned an unsuccessful response", ex);
		}
		catch (OpenAIIoException ex) {
			throw new AiProviderUnavailableException("AI provider is unavailable", ex);
		}
		catch (OpenAIServiceException ex) {
			if (isTemporarilyUnavailable(ex)) {
				throw new AiProviderUnavailableException("AI provider is unavailable", ex);
			}
			throw new AiProviderBadGatewayException("AI provider returned an unsuccessful response", ex);
		}
		catch (AiProviderBadGatewayException | AiProviderUnavailableException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new AiProviderBadGatewayException(withRawResponse("AI provider returned malformed content", content), ex,
					content);
		}
	}

	private Prompt promptFor(String word) {
		ResponseFormat responseFormat = ResponseFormat.builder()
				.type(ResponseFormat.Type.JSON_SCHEMA)
				.jsonSchema(outputConverter.getJsonSchema())
				.build();

		OpenAiChatOptions options = OpenAiChatOptions.builder()
				.model(model)
				.responseFormat(responseFormat)
				.build();

		return new Prompt(List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userPrompt(word))), options);
	}

	private static String userPrompt(String word) {
		return "Generate word info for this German word or fixed expression: " + word;
	}

	private static boolean isTemporarilyUnavailable(OpenAIServiceException ex) {
		return ex.statusCode() == 429 || ex.statusCode() >= 500;
	}

	private static String withRawResponse(String message, String rawResponse) {
		return rawResponse == null ? message : message + "; rawProviderResponse=" + rawResponse;
	}

}

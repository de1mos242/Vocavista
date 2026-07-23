package com.vocavista.backend.media.pronunciation;

import com.openai.errors.OpenAIIoException;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.ObjectMapper;

/** Uses the configured multimodal chat model to score each generated image before it is stored. */
@Component
class SpringAiPhraseImageQualityReviewer implements PhraseImageQualityReviewer {

	private static final ObjectMapper JSON = new ObjectMapper();
	private final ChatModel chatModel;
	private final String apiKey;

	SpringAiPhraseImageQualityReviewer(ChatModel chatModel,
			@Value("${spring.ai.openai.api-key:__missing__}") String apiKey) {
		this.chatModel = chatModel;
		this.apiKey = apiKey;
	}

	@Override
	public ImageQualityReview review(PhraseImagePrompt prompt, GeneratedImage image) {
		if ("__missing__".equals(apiKey) || apiKey.isBlank()) {
			return rejected("Vision review is unavailable because the OpenAI API key is not configured.");
		}
		try {
			String instruction = """
				Review this generated vocabulary image against its brief. Return ONLY JSON with score (0 to 1), relevantToTargetSense,
				anchorsPresent, textArtifactFree, visuallyDistinctive, and rationale. Mark false if the image depicts another sense,
				misses visible anchors, contains readable text/watermarks, or is a generic stock scene. Brief: %s
				""".formatted(prompt.text());
			Media media = Media.builder().mimeType(MimeTypeUtils.parseMimeType(image.contentType())).data(image.bytes()).build();
			String response = chatModel.call(new Prompt(List.of(new SystemMessage("You are a strict visual quality reviewer."),
					UserMessage.builder().text(instruction).media(media).build())))
					.getResult().getOutput().getText().replaceAll("^```(?:json)?\\s*|\\s*```$", "").trim();
			return JSON.readValue(response, ImageQualityReview.class);
		}
		catch (OpenAIIoException | RuntimeException ex) {
			return rejected("Vision review failed: " + ex.getClass().getSimpleName());
		}
	}

	private static ImageQualityReview rejected(String rationale) {
		return new ImageQualityReview(0, false, false, false, false, rationale);
	}
}

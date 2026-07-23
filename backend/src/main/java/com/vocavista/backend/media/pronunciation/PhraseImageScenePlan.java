package com.vocavista.backend.media.pronunciation;

import java.util.List;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/** A visual brief produced before asking Imagen to render a vocabulary sense. */
record PhraseImageScenePlan(
		String targetSense,
		List<String> semanticAnchors,
		String mainAction,
		String compositionGuidance,
		String visualStyle) {

	private static final ObjectMapper JSON = new ObjectMapper();

	PhraseImageScenePlan {
		semanticAnchors = semanticAnchors == null ? List.of() : semanticAnchors.stream()
				.filter(StringUtils::hasText).map(String::trim).toList();
	}

	static PhraseImageScenePlan fromJson(String value) {
		try {
			PhraseImageScenePlan plan = JSON.readValue(value, PhraseImageScenePlan.class);
			if (!StringUtils.hasText(plan.targetSense()) || plan.semanticAnchors().size() < 2
					|| plan.semanticAnchors().size() > 4 || !StringUtils.hasText(plan.mainAction())
					|| !StringUtils.hasText(plan.compositionGuidance()) || !StringUtils.hasText(plan.visualStyle())) {
				throw new IllegalArgumentException("missing required scene-plan fields");
			}
			return plan;
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("scene_provider_error", "OpenAI returned an invalid scene plan", ex);
		}
	}

	String asJson() {
		try {
			return JSON.writeValueAsString(this);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Could not serialize scene plan", ex);
		}
	}
}

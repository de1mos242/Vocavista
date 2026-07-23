package com.vocavista.backend.media.pronunciation;

/**
 * Boundary for a vision-capable image reviewer. Implementations may call an image-model scorer;
 * keeping it separate makes review outcomes deterministic in integration tests.
 */
interface PhraseImageQualityReviewer {

	ImageQualityReview review(PhraseImagePrompt prompt, GeneratedImage image);
}

record ImageQualityReview(double score, boolean relevantToTargetSense, boolean anchorsPresent,
		boolean textArtifactFree, boolean visuallyDistinctive, String rationale) {

	boolean passes(double threshold) {
		return score >= threshold && relevantToTargetSense && anchorsPresent && textArtifactFree && visuallyDistinctive;
	}

	String asJson() {
		return "{\"score\":%.2f,\"relevantToTargetSense\":%s,\"anchorsPresent\":%s,\"textArtifactFree\":%s,\"visuallyDistinctive\":%s,\"rationale\":\"%s\"}"
				.formatted(score, relevantToTargetSense, anchorsPresent, textArtifactFree, visuallyDistinctive,
						rationale == null ? "" : rationale.replace("\\", "\\\\").replace("\"", "\\\""));
	}
}

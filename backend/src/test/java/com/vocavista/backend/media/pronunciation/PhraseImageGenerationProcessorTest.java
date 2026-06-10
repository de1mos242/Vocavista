package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vocavista.backend.wordinfo.WordInfoRecord;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhraseImageGenerationProcessorTest {

	@Test
	void buildsPositiveOnlyImagePrompt() {
		PhraseImagePrompt prompt = PhraseImageGenerationProcessor.promptFor(asset(),
				"A focused student sits at a desk with an open blank notebook, pencil, textbooks, and warm evening light.");

		assertThat(prompt.version()).isEqualTo("v4");
		assertThat(prompt.word()).isEqualTo("Hausaufgabe");
		assertThat(prompt.phrase()).isEqualTo("Ich mache meine Hausaufgabe.");
		assertThat(prompt.text()).contains("focused student", "physical objects", "lighting", "realistic everyday scene");
		assertThat(prompt.text()).doesNotContain("Hausaufgabe", "Ich mache meine Hausaufgabe.", "Phrase context",
				"Vocabulary concept", "Target word", "Full phrase context");
		assertThat(prompt.text()).doesNotContain("Do not", "Avoid", "Never", "without", "no ");
		assertThat(prompt.text()).doesNotContain("labels", "subtitles", "captions", "watermarks", "visible text",
				"text artifacts", "letters", "signs");
	}

	@Test
	void stripsLiteralWordAndPhraseFromSceneDescription() {
		PhraseImagePrompt prompt = PhraseImageGenerationProcessor.promptFor(asset(),
				"Ich mache meine Hausaufgabe A student works on Hausaufgabe at a wooden desk.");

		assertThat(prompt.text()).contains("student works", "wooden desk");
		assertThat(prompt.text()).doesNotContain("Hausaufgabe", "Ich mache meine Hausaufgabe", "mache", "meine");
	}

	private static PhraseImageAsset asset() {
		return PhraseImageAsset.queued(wordInfoRecord(), "Hausaufgabe", "Ich mache meine Hausaufgabe.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe.", "de", "v4", "hash", OffsetDateTime.now());
	}

	private static WordInfoRecord wordInfoRecord() {
		WordInfoRecord record = new WordInfoRecord();
		record.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		record.setNormalizedQuery("hausaufgabe");
		record.setNormalizedWord("Hausaufgabe");
		record.setLanguage("de");
		record.setResponseJson("{}");
		record.setCreatedAt(OffsetDateTime.now());
		record.setUpdatedAt(OffsetDateTime.now());
		return record;
	}

}

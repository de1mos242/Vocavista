package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vocavista.backend.vocabulary.VocabularyItem;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhraseImageGenerationProcessorTest {

	@Test
	void buildsAnchorBearingImagePrompt() {
		PhraseImagePrompt prompt = PhraseImageGenerationProcessor.promptFor(asset(), new PhraseImageScenePlan(
				"doing assigned work at home", java.util.List.of("kitchen table", "worksheet with blank lines", "pencil in hand"),
				"a child solves a worksheet", "over-the-shoulder close view", "warm editorial illustration"));

		assertThat(prompt.version()).isEqualTo("v4");
		assertThat(prompt.word()).isEqualTo("Hausaufgabe");
		assertThat(prompt.phrase()).isEqualTo("Ich mache meine Hausaufgabe.");
		assertThat(prompt.text()).contains("high-quality 16:9 warm editorial illustration image", "doing assigned work at home",
				"kitchen table", "worksheet with blank lines", "pencil in hand", "over-the-shoulder close view");
		assertThat(prompt.text()).doesNotContain("Build a natural", "clean cinematic composition",
				"plausible everyday objects");
		assertThat(prompt.text()).doesNotContain("Hausaufgabe", "Ich mache meine Hausaufgabe.", "Phrase context",
				"Vocabulary concept", "Target word", "Full phrase context");
		assertThat(prompt.text()).doesNotContain("Do not", "Avoid", "Never", "without", "no ");
		assertThat(prompt.text()).doesNotContain("labels", "subtitles", "captions", "watermarks", "visible text",
				"text artifacts", "letters", "signs");
	}

	private static PhraseImageAsset asset() {
		return PhraseImageAsset.queued(vocabularyItem(), "Hausaufgabe", "Ich mache meine Hausaufgabe.", "de", "v4",
				OffsetDateTime.now());
	}

	private static VocabularyItem vocabularyItem() {
		VocabularyItem item = new VocabularyItem();
		item.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		item.setLanguage("de");
		item.setWord("Hausaufgabe");
		item.setPhrase("Ich mache meine Hausaufgabe.");
		item.setPartOfSpeech("noun");
		item.setGender("feminine");
		item.setPlural("Hausaufgaben");
		item.setFrequency("common");
		item.setCompound(true);
		item.setCreatedAt(OffsetDateTime.now());
		item.setUpdatedAt(OffsetDateTime.now());
		return item;
	}

}

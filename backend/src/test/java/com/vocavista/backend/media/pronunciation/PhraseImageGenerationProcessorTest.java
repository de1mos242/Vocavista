package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vocavista.backend.wordinfo.WordInfoRecord;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhraseImageGenerationProcessorTest {

	@Test
	void buildsPositiveOnlyImagePrompt() {
		PhraseImagePrompt prompt = PhraseImageGenerationProcessor.promptFor(asset());

		assertThat(prompt.version()).isEqualTo("v2");
		assertThat(prompt.text()).contains("Phrase context", "Vocabulary concept", "physical objects", "lighting");
		assertThat(prompt.text()).doesNotContain("Do not", "Avoid", "Never", "without", "no ");
		assertThat(prompt.text()).doesNotContain("labels", "subtitles", "captions", "watermarks", "visible text",
				"text artifacts", "letters", "signs");
	}

	private static PhraseImageAsset asset() {
		return PhraseImageAsset.queued(wordInfoRecord(), "Hausaufgabe", "Ich mache meine Hausaufgabe.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe.", "de", "v2", "hash", OffsetDateTime.now());
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

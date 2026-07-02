package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.SaveVocabularyItemRequest;
import com.vocavista.backend.api.model.SaveVocabularyItemResponse;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.VocabularyItemDto;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemMapper;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WordInfoServiceTest {

	private final WordInfoMapper wordInfoMapper = new WordInfoMapper();
	private final VocabularyItemMapper vocabularyItemMapper = new com.vocavista.backend.vocabulary.VocabularyItemMapperImpl();
	private final ProviderWordInfoValidator providerWordInfoValidator = new ProviderWordInfoValidator();

	@Test
	void trimsWordAndMapsProviderResponse() {
		WordInfoService service = new WordInfoService(word -> {
			assertThat(word).isEqualTo("Hausaufgabe");
			return new AiWordInfoResult(SampleWordInfos.nounInfo(), SampleWordInfos.nounInfoJson());
		}, providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("  Hausaufgabe  ");

		assertThat(response.getInputLanguage().getValue()).isEqualTo("de");
		assertThat(response.getMeanings()).hasSize(1);
		assertThat(response.getMeanings().getFirst().getWord()).isEqualTo("Hausaufgabe");
		assertThat(response.getMeanings().getFirst().getPartOfSpeech()).isEqualTo(PartOfSpeech.NOUN);
		assertThat(response.getMeanings().getFirst().getPhraseOptions()).hasSize(3);
		assertThat(response.getMeanings().getFirst().getPhraseOptions()).extracting("phrase")
				.containsExactly("Ich mache meine Hausaufgabe nach dem Abendessen.",
						"Die Hausaufgabe ist heute leicht.", "Hast du die Hausaufgabe schon fertig?");
	}

	@Test
	void mapsMultipleMeaningsForEnglishInput() {
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(SampleWordInfos.englishInputInfo(), "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("home");

		assertThat(response.getInputLanguage().getValue()).isEqualTo("en");
		assertThat(response.getMeanings()).hasSize(2);
		assertThat(response.getMeanings()).extracting("optionId").containsExactly(0, 1);
		assertThat(response.getMeanings()).extracting("word").containsExactly("Hausaufgabe", "Haus");
		assertThat(response.getMeanings().get(1).getPhraseOptions()).extracting("phrase")
				.containsExactly("Das Haus steht am Fluss.", "Wir kaufen ein kleines Haus.",
						"Zu Hause fuehle ich mich wohl.");
	}

	@Test
	void rejectsBlankWordAfterTrimming() {
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(SampleWordInfos.nounInfo(), "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("   ")).isInstanceOf(WordInfoValidationException.class);
	}

	@Test
	void rejectsMalformedProviderResponse() {
		ProviderWordInfo.WordMeaning malformedMeaning = new ProviderWordInfo.WordMeaning("Hausaufgabe", ProviderWordInfo.Language.de,
				new ProviderWordInfo.LocalizedText(List.of("homework"), List.of("домашнее задание")),
				ProviderWordInfo.ProviderPartOfSpeech.noun, Optional.of(ProviderWordInfo.ProviderGender.feminine),
				Optional.of(ProviderWordInfo.ProviderArticle.die), Optional.of("Hausaufgaben"),
				ProviderWordInfo.ProviderFrequency.common, false, List.of(),
				new ProviderWordInfo.LocalizedText(List.of("note"), List.of("заметка")),
				List.of(new ProviderWordInfo.WordExample("Ich mache meine Hausaufgabe.",
						new ProviderWordInfo.LocalizedText(List.of("I do my homework."),
								List.of("Я делаю домашнее задание.")))));
		ProviderWordInfo malformedInfo = new ProviderWordInfo(ProviderWordInfo.InputLanguage.de, List.of(malformedMeaning));
		String rawResponse = "{\"examples\":[{\"sentence\":\"Ich mache meine Hausaufgabe.\"}]}";
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(malformedInfo, rawResponse),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("Hausaufgabe"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("examples must contain at least 3 items")
				.hasMessageContaining("rawProviderResponse=" + rawResponse)
				.extracting(ex -> ((AiProviderBadGatewayException) ex).providerResponse())
				.isEqualTo(rawResponse);
	}

	@Test
	void usesFirstThreeExamplesAsProposedPhrasesWhenProviderReturnsMore() {
		ProviderWordInfo wordInfo = withExtraExample(SampleWordInfos.nounInfo());
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, SampleWordInfos.nounInfoJson()),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("Hausaufgabe");

		assertThat(response.getMeanings().getFirst().getPhraseOptions().getFirst().getPhrase())
				.isEqualTo("Ich mache meine Hausaufgabe nach dem Abendessen.");
		assertThat(response.getMeanings().getFirst().getPhraseOptions()).hasSize(3);
		assertThat(response.getMeanings().getFirst().getPhraseOptions()).extracting("phrase")
				.doesNotContain("Extra sentence that should be ignored.");
	}

	@Test
	void derivesMissingNounArticleFromProviderGender() {
		ProviderWordInfo wordInfo = withArticle(SampleWordInfos.nounInfo(), Optional.empty(),
				Optional.of(ProviderWordInfo.ProviderGender.masculine));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, SampleWordInfos.nounInfoJson()),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("Aufwand");

		assertThat(response.getMeanings().getFirst().getArticle()).isNotNull();
		assertThat(response.getMeanings().getFirst().getPhraseOptions().getFirst().getGender()).isNotNull();
	}

	@Test
	void returnsExistingVocabularyItemWhenSavingSameWordAndPhrase() {
		VocabularyItemDto proposal = wordInfoMapper.toProposedItem(SampleWordInfos.nounMeaning());
		VocabularyItem existingItem = vocabularyItemMapper.toEntity(proposal, UUID.randomUUID(), java.time.OffsetDateTime.now());
		VocabularyItemRepository vocabularyItemRepository = mock(VocabularyItemRepository.class);
		when(vocabularyItemRepository.findFirstByLanguageAndWordIgnoreCaseAndPhraseIgnoreCase(
				proposal.getLanguage(), proposal.getWord(), proposal.getPhrase()))
				.thenReturn(Optional.of(existingItem));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(SampleWordInfos.nounInfo(), "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, vocabularyItemRepository);

		SaveVocabularyItemResponse response = service.saveVocabularyItem(new SaveVocabularyItemRequest(proposal));

		assertThat(response.getItem().getId()).isEqualTo(existingItem.getId());
		verify(vocabularyItemRepository, never()).save(any());
	}

	private static ProviderWordInfo withExtraExample(ProviderWordInfo wordInfo) {
		ProviderWordInfo.WordMeaning meaning = wordInfo.meanings().getFirst();
		List<ProviderWordInfo.WordExample> examples = new java.util.ArrayList<>(meaning.examples());
		examples.add(new ProviderWordInfo.WordExample("Extra sentence that should be ignored.",
				new ProviderWordInfo.LocalizedText(List.of("Ignored extra example."), List.of("Ignoriert."))));
		ProviderWordInfo.WordMeaning updatedMeaning = new ProviderWordInfo.WordMeaning(meaning.normalizedWord(),
				meaning.language(), meaning.translations(), meaning.partOfSpeech(), meaning.gender(), meaning.article(),
				meaning.plural(), meaning.frequency(), meaning.isCompound(), meaning.compoundParts(), meaning.shortNote(),
				examples);
		return new ProviderWordInfo(wordInfo.inputLanguage(), List.of(updatedMeaning));
	}

	private static ProviderWordInfo withArticle(ProviderWordInfo wordInfo,
			Optional<ProviderWordInfo.ProviderArticle> article,
			Optional<ProviderWordInfo.ProviderGender> gender) {
		ProviderWordInfo.WordMeaning meaning = wordInfo.meanings().getFirst();
		ProviderWordInfo.WordMeaning updatedMeaning = new ProviderWordInfo.WordMeaning(meaning.normalizedWord(),
				meaning.language(), meaning.translations(), meaning.partOfSpeech(), gender, article, meaning.plural(),
				meaning.frequency(), meaning.isCompound(), meaning.compoundParts(), meaning.shortNote(), meaning.examples());
		return new ProviderWordInfo(wordInfo.inputLanguage(), List.of(updatedMeaning));
	}

	private static VocabularyItemRepository emptyRepository() {
		VocabularyItemRepository vocabularyItemRepository = mock(VocabularyItemRepository.class);
		when(vocabularyItemRepository.findByLanguageAndWordIgnoreCase("de", "Hausaufgabe")).thenReturn(List.of());
		return vocabularyItemRepository;
	}

}

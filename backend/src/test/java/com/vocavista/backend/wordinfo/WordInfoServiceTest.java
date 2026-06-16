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
		VocabularyItemRepository vocabularyItemRepository = emptyRepository();
		WordInfoService service = new WordInfoService(word -> {
			assertThat(word).isEqualTo("Hausaufgabe");
			return new AiWordInfoResult(SampleWordInfos.nounInfo(), SampleWordInfos.nounInfoJson());
		}, providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, vocabularyItemRepository);

		WordInfoResponse response = service.getWordInfo("  Hausaufgabe  ");

		assertThat(response.getCanonicalWord()).isEqualTo("Hausaufgabe");
		assertThat(response.getExistingItems()).isEmpty();
		assertThat(response.getProposedItem().getPartOfSpeech()).isEqualTo(PartOfSpeech.NOUN);
		assertThat(response.getProposedItem().getPhrase()).isEqualTo("Ich mache meine Hausaufgabe nach dem Abendessen.");
		assertThat(response.getProposedItems()).hasSize(3);
		assertThat(response.getProposedItems()).extracting("phrase")
				.containsExactly("Ich mache meine Hausaufgabe nach dem Abendessen.",
						"Die Hausaufgabe ist heute leicht.", "Hast du die Hausaufgabe schon fertig?");
	}

	@Test
	void rejectsBlankWordAfterTrimming() {
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(SampleWordInfos.nounInfo(), "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("   ")).isInstanceOf(WordInfoValidationException.class);
	}

	@Test
	void rejectsMalformedProviderResponse() {
		ProviderWordInfo malformedInfo = new ProviderWordInfo("Hausaufgabe", ProviderWordInfo.Language.de,
				new ProviderWordInfo.LocalizedText(List.of("homework"), List.of("домашнее задание")),
				ProviderWordInfo.ProviderPartOfSpeech.noun, Optional.of(ProviderWordInfo.ProviderGender.feminine),
				Optional.of(ProviderWordInfo.ProviderArticle.die), Optional.of("Hausaufgaben"),
				ProviderWordInfo.ProviderFrequency.common, false, List.of(),
				new ProviderWordInfo.LocalizedText(List.of("note"), List.of("заметка")),
				List.of(new ProviderWordInfo.WordExample("Ich mache meine Hausaufgabe.",
						new ProviderWordInfo.LocalizedText(List.of("I do my homework."),
								List.of("Я делаю домашнее задание.")))));
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

		assertThat(response.getProposedItem().getPhrase()).isEqualTo("Ich mache meine Hausaufgabe nach dem Abendessen.");
		assertThat(response.getProposedItems()).hasSize(3);
		assertThat(response.getProposedItems()).extracting("phrase").doesNotContain("Extra sentence that should be ignored.");
	}

	@Test
	void derivesMissingNounArticleFromProviderGender() {
		ProviderWordInfo wordInfo = withArticle(SampleWordInfos.nounInfo(), Optional.empty(),
				Optional.of(ProviderWordInfo.ProviderGender.masculine));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, SampleWordInfos.nounInfoJson()),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("Aufwand");

		assertThat(response.getProposedItem().getGender()).isNotNull();
	}

	@Test
	void returnsExistingVocabularyItemWhenSavingSameWordAndPhrase() {
		VocabularyItemDto proposal = wordInfoMapper.toProposedItem(SampleWordInfos.nounInfo());
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
		List<ProviderWordInfo.WordExample> examples = new java.util.ArrayList<>(wordInfo.examples());
		examples.add(new ProviderWordInfo.WordExample("Extra sentence that should be ignored.",
				new ProviderWordInfo.LocalizedText(List.of("Ignored extra example."), List.of("Ignoriert."))));
		return new ProviderWordInfo(wordInfo.normalizedWord(), wordInfo.language(), wordInfo.translations(),
				wordInfo.partOfSpeech(), wordInfo.gender(), wordInfo.article(), wordInfo.plural(), wordInfo.frequency(),
				wordInfo.isCompound(), wordInfo.compoundParts(), wordInfo.shortNote(), examples);
	}

	private static ProviderWordInfo withArticle(ProviderWordInfo wordInfo,
			Optional<ProviderWordInfo.ProviderArticle> article,
			Optional<ProviderWordInfo.ProviderGender> gender) {
		return new ProviderWordInfo(wordInfo.normalizedWord(), wordInfo.language(), wordInfo.translations(),
				wordInfo.partOfSpeech(), gender, article, wordInfo.plural(), wordInfo.frequency(), wordInfo.isCompound(),
				wordInfo.compoundParts(), wordInfo.shortNote(), wordInfo.examples());
	}

	private static VocabularyItemRepository emptyRepository() {
		VocabularyItemRepository vocabularyItemRepository = mock(VocabularyItemRepository.class);
		when(vocabularyItemRepository.findByLanguageAndWordIgnoreCase("de", "Hausaufgabe")).thenReturn(List.of());
		return vocabularyItemRepository;
	}

}

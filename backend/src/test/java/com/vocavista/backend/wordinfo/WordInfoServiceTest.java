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
import java.util.Map;
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
		assertThat(response.getMeanings()).extracting("gloss")
				.containsExactly(Map.of("en", List.of("school work assigned to a student"), "ru", List.of("zadanie dlya shkoly")),
						Map.of("en", List.of("a residential building"), "ru", List.of("zhiloe zdanie")));
		assertThat(response.getMeanings().get(1).getPhraseOptions()).extracting("phrase")
				.containsExactly("Das Haus steht am Fluss.", "Wir kaufen ein kleines Haus.",
						"Zu Hause fuehle ich mich wohl.");
	}

	@Test
	void rejectsDuplicateNormalizedGermanCandidatesAndGlosses() {
		ProviderWordInfo.WordMeaning original = SampleWordInfos.nounMeaning();
		ProviderWordInfo.WordMeaning duplicateCandidate = new ProviderWordInfo.WordMeaning(" hausaufgabe ", original.language(),
				original.translations(), localizedText("a different label", "drugaya metka"), original.partOfSpeech(),
				original.gender(), original.article(), original.plural(), original.frequency(), original.isCompound(),
				original.compoundParts(), original.shortNote(), original.examples());

		assertThatThrownBy(() -> providerWordInfoValidator.validate(new ProviderWordInfo(ProviderWordInfo.InputLanguage.de,
				List.of(original, duplicateCandidate))))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("duplicate normalized German candidates");

		ProviderWordInfo.WordMeaning duplicateGloss = new ProviderWordInfo.WordMeaning("Schularbeit", original.language(),
				original.translations(), original.gloss(), original.partOfSpeech(), original.gender(), original.article(),
				original.plural(), original.frequency(), original.isCompound(), original.compoundParts(), original.shortNote(),
				original.examples());
		assertThatThrownBy(() -> providerWordInfoValidator.validate(new ProviderWordInfo(ProviderWordInfo.InputLanguage.de,
				List.of(original, duplicateGloss))))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("duplicate localized glosses");
	}

	@Test
	void rejectsEnglishInputLeakingIntoGermanExamples() {
		ProviderWordInfo wordInfo = new ProviderWordInfo(ProviderWordInfo.InputLanguage.en, List.of(
				new ProviderWordInfo.WordMeaning(
						"Apfel",
						ProviderWordInfo.Language.de,
						new ProviderWordInfo.LocalizedText(List.of("apple"), List.of("yabloko")),
						new ProviderWordInfo.LocalizedText(List.of("edible fruit"), List.of("syedobnyy frukt")),
						ProviderWordInfo.ProviderPartOfSpeech.noun,
						Optional.of(ProviderWordInfo.ProviderGender.masculine),
						Optional.of(ProviderWordInfo.ProviderArticle.der),
						Optional.of("Aepfel"),
						ProviderWordInfo.ProviderFrequency.common,
						false,
						List.of(),
						new ProviderWordInfo.LocalizedText(List.of("A common fruit."), List.of("Frukt.")),
						List.of(
								new ProviderWordInfo.WordExample("Er isst apple.",
										new ProviderWordInfo.LocalizedText(List.of("He eats an apple."), List.of("On est yabloko."))),
								new ProviderWordInfo.WordExample("Der Apfel ist rot.",
										new ProviderWordInfo.LocalizedText(List.of("The apple is red."), List.of("Yabloko krasnoe."))),
								new ProviderWordInfo.WordExample("Ich kaufe einen Apfel.",
										new ProviderWordInfo.LocalizedText(List.of("I buy an apple."), List.of("Ya pokupayu yabloko.")))))));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("apple"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("German examples must not contain the source-language input word");
	}

	@Test
	void rejectsExamplesThatDoNotUseTheDeclaredSenseCandidate() {
		ProviderWordInfo.WordMeaning meaning = SampleWordInfos.nounMeaning();
		List<ProviderWordInfo.WordExample> unrelatedExamples = List.of(
				new ProviderWordInfo.WordExample("Der Unterricht beginnt um acht.", localizedText("School begins at eight.", "Uroki nachinayutsya v vosem.")),
				new ProviderWordInfo.WordExample("Die Lehrerin erklärt die Aufgabe.", localizedText("The teacher explains the task.", "Uchitel obyasnyaet zadanie.")),
				new ProviderWordInfo.WordExample("Die Klasse ist heute ruhig.", localizedText("The class is quiet today.", "Klass segodnya tikhiy.")));
		ProviderWordInfo.WordMeaning withUnrelatedExamples = new ProviderWordInfo.WordMeaning(meaning.normalizedWord(),
				meaning.language(), meaning.translations(), meaning.gloss(), meaning.partOfSpeech(), meaning.gender(),
				meaning.article(), meaning.plural(), meaning.frequency(), meaning.isCompound(), meaning.compoundParts(),
				meaning.shortNote(), unrelatedExamples);

		assertThatThrownBy(() -> providerWordInfoValidator.validate(new ProviderWordInfo(ProviderWordInfo.InputLanguage.de,
				List.of(withUnrelatedExamples))))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("German examples must use the declared German meaning candidate");
	}

	@Test
	void allowsSourceInputWhenItIsAlsoTheGermanWord() {
		ProviderWordInfo wordInfo = new ProviderWordInfo(ProviderWordInfo.InputLanguage.en, List.of(
				new ProviderWordInfo.WordMeaning(
						"Taxi",
						ProviderWordInfo.Language.de,
						new ProviderWordInfo.LocalizedText(List.of("taxi"), List.of("taksi")),
						new ProviderWordInfo.LocalizedText(List.of("car for hire"), List.of("mashina po vyzovu")),
						ProviderWordInfo.ProviderPartOfSpeech.noun,
						Optional.of(ProviderWordInfo.ProviderGender.neuter),
						Optional.of(ProviderWordInfo.ProviderArticle.das),
						Optional.of("Taxis"),
						ProviderWordInfo.ProviderFrequency.common,
						false,
						List.of(),
						new ProviderWordInfo.LocalizedText(List.of("A car for hire."), List.of("Mashina po vyzovu.")),
						List.of(
								new ProviderWordInfo.WordExample("Das Taxi wartet draussen.",
										new ProviderWordInfo.LocalizedText(List.of("The taxi waits outside."), List.of("Taksi zhdet snaruzhi."))),
								new ProviderWordInfo.WordExample("Ich rufe ein Taxi.",
										new ProviderWordInfo.LocalizedText(List.of("I call a taxi."), List.of("Ya vyzyvayu taksi."))),
								new ProviderWordInfo.WordExample("Wir fahren mit dem Taxi.",
										new ProviderWordInfo.LocalizedText(List.of("We go by taxi."), List.of("My edem na taksi.")))))));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, "{}"),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("taxi");

		assertThat(response.getMeanings().getFirst().getWord()).isEqualTo("Taxi");
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
				new ProviderWordInfo.LocalizedText(List.of("school assignment"), List.of("школьное задание")),
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
	void correctsNounArticleWhenProviderArticleDoesNotMatchGender() {
		ProviderWordInfo wordInfo = withArticle(SampleWordInfos.nounInfo(), Optional.of(ProviderWordInfo.ProviderArticle.der),
				Optional.of(ProviderWordInfo.ProviderGender.feminine));
		WordInfoService service = new WordInfoService(word -> new AiWordInfoResult(wordInfo, SampleWordInfos.nounInfoJson()),
				providerWordInfoValidator, wordInfoMapper, vocabularyItemMapper, emptyRepository());

		WordInfoResponse response = service.getWordInfo("Hausaufgabe");

		assertThat(response.getMeanings().getFirst().getArticle().getValue()).isEqualTo("die");
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
				meaning.language(), meaning.translations(), meaning.gloss(), meaning.partOfSpeech(), meaning.gender(), meaning.article(),
				meaning.plural(), meaning.frequency(), meaning.isCompound(), meaning.compoundParts(), meaning.shortNote(),
				examples);
		return new ProviderWordInfo(wordInfo.inputLanguage(), List.of(updatedMeaning));
	}

	private static ProviderWordInfo withArticle(ProviderWordInfo wordInfo,
			Optional<ProviderWordInfo.ProviderArticle> article,
			Optional<ProviderWordInfo.ProviderGender> gender) {
		ProviderWordInfo.WordMeaning meaning = wordInfo.meanings().getFirst();
		ProviderWordInfo.WordMeaning updatedMeaning = new ProviderWordInfo.WordMeaning(meaning.normalizedWord(),
				meaning.language(), meaning.translations(), meaning.gloss(), meaning.partOfSpeech(), gender, article, meaning.plural(),
				meaning.frequency(), meaning.isCompound(), meaning.compoundParts(), meaning.shortNote(), meaning.examples());
		return new ProviderWordInfo(wordInfo.inputLanguage(), List.of(updatedMeaning));
	}

	private static VocabularyItemRepository emptyRepository() {
		VocabularyItemRepository vocabularyItemRepository = mock(VocabularyItemRepository.class);
		when(vocabularyItemRepository.findByLanguageAndWordIgnoreCase("de", "Hausaufgabe")).thenReturn(List.of());
		return vocabularyItemRepository;
	}

	private static ProviderWordInfo.LocalizedText localizedText(String en, String ru) {
		return new ProviderWordInfo.LocalizedText(List.of(en), List.of(ru));
	}

}

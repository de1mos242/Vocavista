package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.GermanArticle;
import com.vocavista.backend.api.model.InputLanguage;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.VocabularyItemDto;
import com.vocavista.backend.api.model.VocabularyItemTranslation;
import com.vocavista.backend.api.model.WordMeaningOption;
import com.vocavista.backend.api.model.WordFrequency;
import com.vocavista.backend.vocabulary.VocabularyItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class WordInfoMapper {

	List<WordMeaningOption> toMeaningOptions(ProviderWordInfo wordInfo) {
		return wordInfo.meanings().stream()
				.limit(5)
				.map(this::toMeaningOption)
				.toList();
	}

	InputLanguage toInputLanguage(ProviderWordInfo wordInfo) {
		return InputLanguage.fromValue(wordInfo.inputLanguage().name());
	}

	private WordMeaningOption toMeaningOption(ProviderWordInfo.WordMeaning meaning) {
		List<VocabularyItemDto> phraseOptions = meaning.examples().stream()
				.limit(3)
				.map(example -> toProposedItem(meaning, example))
				.toList();
		return new WordMeaningOption(
				0,
				trim(meaning.normalizedWord()),
				meaning.language().name(),
				localizedText(meaning.translations()),
				phraseOptions,
				PartOfSpeech.fromValue(meaning.partOfSpeech().name()),
				WordFrequency.fromValue(meaning.frequency().name()),
				Boolean.TRUE.equals(meaning.isCompound()))
				.gender(gender(meaning.gender()))
				.article(article(meaning.article()))
				.plural(optionalString(meaning.plural()));
	}

	VocabularyItemDto toProposedItem(ProviderWordInfo.WordMeaning meaning) {
		return toProposedItem(meaning, meaning.examples().getFirst());
	}

	private VocabularyItemDto toProposedItem(ProviderWordInfo.WordMeaning meaning, ProviderWordInfo.WordExample example) {
		return new VocabularyItemDto(
				trim(meaning.normalizedWord()),
				trim(example.sentence()),
				meaning.language().name(),
				translations(meaning.translations(), example.translations()),
				PartOfSpeech.fromValue(meaning.partOfSpeech().name()),
				WordFrequency.fromValue(meaning.frequency().name()),
				Boolean.TRUE.equals(meaning.isCompound()))
				.gender(gender(meaning.gender()))
				.plural(optionalString(meaning.plural()));
	}

	VocabularyItemDto toApiItem(VocabularyItem item) {
		return new VocabularyItemDto(
				item.getWord(),
				item.getPhrase(),
				item.getLanguage(),
				item.getTranslations().stream()
						.map(translation -> new VocabularyItemTranslation(translation.getLanguage(),
								translation.getWordTranslation(), translation.getPhraseTranslation()))
						.toList(),
				PartOfSpeech.fromValue(item.getPartOfSpeech()),
				WordFrequency.fromValue(item.getFrequency()),
				item.isCompound())
				.id(item.getId())
				.gender(item.getGender() == null ? null : Gender.fromValue(item.getGender()))
				.plural(item.getPlural());
	}

	private static List<VocabularyItemTranslation> translations(
			ProviderWordInfo.LocalizedText wordTranslations,
			ProviderWordInfo.LocalizedText phraseTranslations) {
		List<VocabularyItemTranslation> values = new ArrayList<>();
		addTranslation(values, "en", first(wordTranslations.en()), first(phraseTranslations.en()));
		addTranslation(values, "ru", first(wordTranslations.ru()), first(phraseTranslations.ru()));
		return values;
	}

	private static Map<String, List<String>> localizedText(ProviderWordInfo.LocalizedText text) {
		Map<String, List<String>> values = new LinkedHashMap<>();
		values.put("en", text.en().stream().map(WordInfoMapper::trim).toList());
		values.put("ru", text.ru().stream().map(WordInfoMapper::trim).toList());
		return values;
	}

	private static void addTranslation(List<VocabularyItemTranslation> values, String language, String word, String phrase) {
		if (StringUtils.hasText(word) && StringUtils.hasText(phrase)) {
			values.add(new VocabularyItemTranslation(language, trim(word), trim(phrase)));
		}
	}

	private static String first(List<String> values) {
		return values == null || values.isEmpty() ? null : values.getFirst();
	}

	private static Gender gender(Optional<ProviderWordInfo.ProviderGender> value) {
		return value == null || value.isEmpty() ? null : Gender.fromValue(value.get().name());
	}

	private static GermanArticle article(Optional<ProviderWordInfo.ProviderArticle> value) {
		return value == null || value.isEmpty() ? null : GermanArticle.fromValue(value.get().name());
	}

	private static String optionalString(Optional<String> value) {
		return value == null || value.isEmpty() ? null : trim(value.get());
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

}

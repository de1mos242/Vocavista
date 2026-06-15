package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.VocabularyItemDto;
import com.vocavista.backend.api.model.VocabularyItemTranslation;
import com.vocavista.backend.api.model.WordFrequency;
import com.vocavista.backend.vocabulary.VocabularyItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class WordInfoMapper {

	VocabularyItemDto toProposedItem(ProviderWordInfo wordInfo) {
		ProviderWordInfo.WordExample example = wordInfo.examples().getFirst();
		return new VocabularyItemDto(
				trim(wordInfo.normalizedWord()),
				trim(example.sentence()),
				wordInfo.language().name(),
				translations(wordInfo.translations(), example.translations()),
				PartOfSpeech.fromValue(wordInfo.partOfSpeech().name()),
				WordFrequency.fromValue(wordInfo.frequency().name()),
				Boolean.TRUE.equals(wordInfo.isCompound()))
				.gender(gender(wordInfo.gender()))
				.plural(optionalString(wordInfo.plural()));
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

	private static String optionalString(Optional<String> value) {
		return value == null || value.isEmpty() ? null : trim(value.get());
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

}

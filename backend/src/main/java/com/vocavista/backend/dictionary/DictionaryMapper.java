package com.vocavista.backend.dictionary;

import com.vocavista.backend.api.model.DictionaryReviewItem;
import com.vocavista.backend.api.model.DictionaryVideoManifestItem;
import com.vocavista.backend.api.model.GermanArticle;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.media.MediaAssetQueryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class DictionaryMapper {

	DictionaryReviewItem toReviewItem(UserDictionaryEntry entry, String expectedAnswer) {
		VocabularyItem item = entry.getVocabularyItem();
		DictionaryReviewItem reviewItem = new DictionaryReviewItem(entry.getId(), item.getId(), item.getWord(),
				expectedAnswer, wordTranslations(item), phraseTranslations(item),
				PartOfSpeech.fromValue(item.getPartOfSpeech()), entry.getDueAt());
		reviewItem.setArticle(articleFor(item));
		reviewItem.setPhrase(item.getPhrase());
		return reviewItem;
	}

	DictionaryVideoManifestItem toVideoManifestItem(MediaAssetQueryService.CompletedPronunciation pronunciation) {
		return new DictionaryVideoManifestItem(pronunciation.id(), pronunciation.wordInfoId(), pronunciation.videoUrl(),
				pronunciation.fullVideoUrl(), pronunciation.updatedAt());
	}

	private static Map<String, List<String>> wordTranslations(VocabularyItem item) {
		return item.getTranslations().stream()
				.collect(java.util.stream.Collectors.toMap(
						translation -> translation.getLanguage(),
						translation -> List.of(translation.getWordTranslation()),
						(first, ignored) -> first));
	}

	private static Map<String, List<String>> phraseTranslations(VocabularyItem item) {
		return item.getTranslations().stream()
				.collect(java.util.stream.Collectors.toMap(
						translation -> translation.getLanguage(),
						translation -> List.of(translation.getPhraseTranslation()),
						(first, ignored) -> first));
	}

	private static GermanArticle articleFor(VocabularyItem item) {
		if (!"noun".equals(item.getPartOfSpeech()) || item.getGender() == null) {
			return null;
		}
		return switch (item.getGender()) {
			case "masculine" -> GermanArticle.DER;
			case "feminine" -> GermanArticle.DIE;
			case "neuter" -> GermanArticle.DAS;
			default -> null;
		};
	}

}

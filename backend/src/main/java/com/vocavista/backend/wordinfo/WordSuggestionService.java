package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.api.model.WordSuggestion;
import com.vocavista.backend.api.model.WordSuggestionsResponse;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import java.net.URI;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordSuggestionService {

	private static final int MAX_QUERY_LENGTH = 80;

	private final WordInfoRepository wordInfoRepository;
	private final PronunciationRepository pronunciationRepository;

	WordSuggestionsResponse search(String query) {
		String normalizedQuery = trimAndValidate(query);
		LinkedHashMap<String, WordSuggestion> suggestions = new LinkedHashMap<>();

		for (WordInfoRecord record : wordInfoRepository
				.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(normalizedQuery)) {
			WordSuggestion suggestion = new WordSuggestion(record.getNormalizedWord(), WordSuggestion.SourceEnum.WORD_INFO);
			suggestion.setWordInfoId(record.getId());
			suggestions.putIfAbsent(key(record.getNormalizedWord(), null), suggestion);
		}

		for (PronunciationAsset asset : pronunciationRepository
				.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(normalizedQuery)) {
			WordSuggestion suggestion = new WordSuggestion(asset.getNormalizedWord(), WordSuggestion.SourceEnum.PRONUNCIATION);
			suggestion.setPhrase(asset.getNormalizedPhrase());
			suggestion.setWordInfoId(asset.getWordInfoRecord().getId());
			suggestion.setPronunciationId(asset.getId());
			suggestion.setStatus(PronunciationStatus.fromValue(asset.getStatus().name().toLowerCase()));
			if (asset.getStatus() == PronunciationAssetStatus.COMPLETED && StringUtils.hasText(asset.getVideoObjectKey())) {
				suggestion.setVideoUrl(URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video/small"));
				suggestion.setFullVideoUrl(URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video"));
			}
			suggestions.putIfAbsent(key(asset.getNormalizedWord(), asset.getNormalizedPhrase()), suggestion);
		}

		return new WordSuggestionsResponse(suggestions.values().stream().toList());
	}

	private static String trimAndValidate(String query) {
		String trimmedQuery = query == null ? "" : query.trim().replaceAll("\\s+", " ");
		if (!StringUtils.hasText(trimmedQuery)) {
			throw new WordInfoValidationException("query must not be blank");
		}
		if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
			throw new WordInfoValidationException("query must not exceed 80 characters");
		}
		return trimmedQuery;
	}

	private static String key(String word, String phrase) {
		return word.toLowerCase() + "\0" + (phrase == null ? "" : phrase.toLowerCase());
	}

}

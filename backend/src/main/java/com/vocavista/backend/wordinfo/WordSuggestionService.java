package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.api.model.WordSuggestion;
import com.vocavista.backend.api.model.WordSuggestionsResponse;
import com.vocavista.backend.media.MediaAssetQueryService;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordSuggestionService {

	private static final int MAX_QUERY_LENGTH = 80;

	private final WordInfoRepository wordInfoRepository;
	private final MediaAssetQueryService mediaAssetQueryService;

	WordSuggestionsResponse search(String query) {
		String normalizedQuery = trimAndValidate(query);
		LinkedHashMap<String, WordSuggestion> suggestions = new LinkedHashMap<>();

		for (WordInfoRecord record : wordInfoRepository
				.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(normalizedQuery)) {
			WordSuggestion suggestion = new WordSuggestion(record.getNormalizedWord(), WordSuggestion.SourceEnum.WORD_INFO);
			suggestion.setWordInfoId(record.getId());
			suggestions.putIfAbsent(key(record.getNormalizedWord(), null), suggestion);
		}

		for (MediaAssetQueryService.PronunciationSuggestion pronunciation : mediaAssetQueryService
				.findPronunciationSuggestions(normalizedQuery)) {
			WordSuggestion suggestion = new WordSuggestion(pronunciation.word(), WordSuggestion.SourceEnum.PRONUNCIATION);
			suggestion.setPhrase(pronunciation.phrase());
			suggestion.setWordInfoId(pronunciation.wordInfoId());
			suggestion.setPronunciationId(pronunciation.pronunciationId());
			suggestion.setStatus(PronunciationStatus.fromValue(pronunciation.status()));
			suggestion.setVideoUrl(pronunciation.videoUrl());
			suggestion.setFullVideoUrl(pronunciation.fullVideoUrl());
			suggestions.putIfAbsent(key(pronunciation.word(), pronunciation.phrase()), suggestion);
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

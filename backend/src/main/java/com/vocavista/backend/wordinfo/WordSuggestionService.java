package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.WordSuggestion;
import com.vocavista.backend.api.model.WordSuggestionsResponse;
import com.vocavista.backend.media.MediaAssetQueryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordSuggestionService {

	private static final int MAX_QUERY_LENGTH = 80;

	private static final String SOURCE_LANGUAGE = "de";

	private final VocabularyItemRepository vocabularyItemRepository;
	private final MediaAssetQueryService mediaAssetQueryService;
	private final WordSuggestionMapper wordSuggestionMapper;

	WordSuggestionsResponse search(String query) {
		String normalizedQuery = trimAndValidate(query);
		LinkedHashMap<String, WordSuggestion> suggestions = new LinkedHashMap<>();

		for (VocabularyItem item : vocabularyItemRepository
				.findTop10ByLanguageAndWordContainingIgnoreCase(SOURCE_LANGUAGE, normalizedQuery, PageRequest.of(0, 10))) {
			WordSuggestion suggestion = wordSuggestionMapper.toVocabularyItemSuggestion(item);
			suggestions.putIfAbsent(key(item.getWord(), item.getPhrase()), suggestion);
		}

		for (MediaAssetQueryService.PronunciationSuggestion pronunciation : mediaAssetQueryService
				.findPronunciationSuggestions(normalizedQuery)) {
			WordSuggestion suggestion = wordSuggestionMapper.toPronunciationSuggestion(pronunciation);
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

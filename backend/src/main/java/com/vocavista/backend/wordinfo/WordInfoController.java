package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.WordsApi;
import com.vocavista.backend.api.model.SaveVocabularyItemRequest;
import com.vocavista.backend.api.model.SaveVocabularyItemResponse;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.api.model.WordSuggestionsResponse;
import com.vocavista.backend.auth.RequireFunctionalAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequireFunctionalAccess
class WordInfoController implements WordsApi {

	private final WordInfoService wordInfoService;
	private final WordSuggestionService wordSuggestionService;

	@Override
	public ResponseEntity<WordInfoResponse> getWordInfo(String word) {
		return ResponseEntity.ok(wordInfoService.getWordInfo(word));
	}

	@Override
	public ResponseEntity<SaveVocabularyItemResponse> saveVocabularyItem(
			SaveVocabularyItemRequest saveVocabularyItemRequest) {
		return ResponseEntity.ok(wordInfoService.saveVocabularyItem(saveVocabularyItemRequest));
	}

	@Override
	public ResponseEntity<WordSuggestionsResponse> getWordSuggestions(String query) {
		return ResponseEntity.ok(wordSuggestionService.search(query));
	}

}

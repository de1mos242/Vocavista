package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.WordsApi;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.api.model.WordSuggestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class WordInfoController implements WordsApi {

	private final WordInfoService wordInfoService;
	private final WordSuggestionService wordSuggestionService;

	@Override
	public ResponseEntity<WordInfoResponse> getWordInfo(String word) {
		return ResponseEntity.ok(wordInfoService.getWordInfo(word));
	}

	@Override
	public ResponseEntity<WordSuggestionsResponse> getWordSuggestions(String query) {
		return ResponseEntity.ok(wordSuggestionService.search(query));
	}

}

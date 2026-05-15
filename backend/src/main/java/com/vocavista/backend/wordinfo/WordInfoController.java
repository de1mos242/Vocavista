package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.WordsApi;
import com.vocavista.backend.api.model.WordInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class WordInfoController implements WordsApi {

	private final WordInfoService wordInfoService;

	@Override
	public ResponseEntity<WordInfoResponse> getWordInfo(String word) {
		return ResponseEntity.ok(wordInfoService.getWordInfo(word));
	}

}

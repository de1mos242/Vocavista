package com.vocavista.backend.dictionary;

import com.vocavista.backend.api.DictionaryApi;
import com.vocavista.backend.api.model.DictionaryReviewResponse;
import com.vocavista.backend.api.model.DictionaryReviewSubmitRequest;
import com.vocavista.backend.api.model.DictionaryReviewSubmitResponse;
import com.vocavista.backend.api.model.DictionaryVideoManifestResponse;
import com.vocavista.backend.auth.RequireFunctionalAccess;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequireFunctionalAccess
class DictionaryController implements DictionaryApi {

	private final UserDictionaryService userDictionaryService;

	@Override
	public ResponseEntity<DictionaryReviewResponse> getDictionaryReview(Integer limit, Boolean includeUpcoming) {
		return ResponseEntity.ok(userDictionaryService.getReviewItems(limit, includeUpcoming));
	}

	@Override
	public ResponseEntity<DictionaryReviewSubmitResponse> submitDictionaryReview(UUID entryId,
			DictionaryReviewSubmitRequest dictionaryReviewSubmitRequest) {
		return ResponseEntity.ok(userDictionaryService.submitReview(entryId, dictionaryReviewSubmitRequest));
	}

	@Override
	public ResponseEntity<DictionaryVideoManifestResponse> getDictionaryVideos() {
		return ResponseEntity.ok(userDictionaryService.getVideoManifest());
	}

}

package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.MediaApi;
import com.vocavista.backend.api.model.PronunciationVideoRequest;
import com.vocavista.backend.api.model.PronunciationVideoResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class PronunciationVideoController implements MediaApi {

	private final PronunciationVideoService pronunciationVideoService;

	@Override
	public ResponseEntity<PronunciationVideoResponse> createPronunciationVideo(
			PronunciationVideoRequest pronunciationVideoRequest) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(pronunciationVideoService.create(pronunciationVideoRequest));
	}

	@Override
	public ResponseEntity<PronunciationVideoResponse> getPronunciationVideo(UUID id) {
		return ResponseEntity.ok(pronunciationVideoService.get(id));
	}

}

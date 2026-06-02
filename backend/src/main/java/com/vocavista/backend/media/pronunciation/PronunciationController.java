package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.MediaApi;
import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.auth.RequireFunctionalAccess;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequireFunctionalAccess
class PronunciationController implements MediaApi {

	private final PronunciationService pronunciationService;

	@Override
	public ResponseEntity<PronunciationResponse> createPronunciation(PronunciationRequest pronunciationRequest) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(pronunciationService.create(pronunciationRequest));
	}

	@Override
	public ResponseEntity<PronunciationResponse> getPronunciation(UUID id) {
		return ResponseEntity.ok(pronunciationService.get(id));
	}

	@Override
	public ResponseEntity<Resource> getPronunciationVideo(UUID id) {
		StoredMedia video = pronunciationService.getVideo(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(video.contentType() == null ? "video/mp4" : video.contentType()))
				.contentLength(video.bytes().length)
				.body(new ByteArrayResource(video.bytes()));
	}

}

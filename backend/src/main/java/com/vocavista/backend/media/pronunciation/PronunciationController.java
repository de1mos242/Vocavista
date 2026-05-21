package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.MediaApi;
import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
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

	@GetMapping("/api/v1/media/pronunciations/{id}/audio")
	ResponseEntity<byte[]> getPronunciationAudio(@PathVariable UUID id) {
		StoredMedia audio = pronunciationService.getAudio(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(audio.contentType() == null ? "audio/mpeg" : audio.contentType()))
				.body(audio.bytes());
	}

}

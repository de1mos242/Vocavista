package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.MediaApi;
import com.vocavista.backend.api.model.PhraseImageRequest;
import com.vocavista.backend.api.model.PhraseImageResponse;
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
	private final PhraseImageService phraseImageService;

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

	@Override
	public ResponseEntity<Resource> getSmallPronunciationVideo(UUID id) {
		StoredMedia video = pronunciationService.getSmallVideo(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(video.contentType() == null ? "video/mp4" : video.contentType()))
				.contentLength(video.bytes().length)
				.body(new ByteArrayResource(video.bytes()));
	}

	@Override
	public ResponseEntity<PhraseImageResponse> createPhraseImage(PhraseImageRequest phraseImageRequest) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(phraseImageService.create(phraseImageRequest));
	}

	@Override
	public ResponseEntity<PhraseImageResponse> getPhraseImage(UUID id) {
		return ResponseEntity.ok(phraseImageService.get(id));
	}

	@Override
	public ResponseEntity<Resource> getPhraseImageBytes(UUID id) {
		StoredMedia image = phraseImageService.getImage(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType() == null ? "image/png" : image.contentType()))
				.contentLength(image.bytes().length)
				.body(new ByteArrayResource(image.bytes()));
	}

	@Override
	public ResponseEntity<Resource> getPhraseImageCandidateBytes(UUID id, Integer candidateIndex) {
		StoredMedia image = phraseImageService.getCandidateImage(id, candidateIndex);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType() == null ? "image/png" : image.contentType()))
				.contentLength(image.bytes().length)
				.body(new ByteArrayResource(image.bytes()));
	}

	@Override
	public ResponseEntity<PhraseImageResponse> selectPhraseImageCandidate(UUID id, Integer candidateIndex) {
		return ResponseEntity.ok(phraseImageService.selectCandidate(id, candidateIndex));
	}

}

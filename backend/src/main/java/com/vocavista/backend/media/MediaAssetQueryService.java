package com.vocavista.backend.media;

import com.vocavista.backend.media.pronunciation.PhraseImageAsset;
import com.vocavista.backend.media.pronunciation.PhraseImageAssetStatus;
import com.vocavista.backend.media.pronunciation.PhraseImageRepository;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MediaAssetQueryService {

	private final PronunciationRepository pronunciationRepository;
	private final PhraseImageRepository phraseImageRepository;

	@Transactional(readOnly = true)
	public List<PronunciationSuggestion> findPronunciationSuggestions(String query) {
		return pronunciationRepository.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(query)
				.stream()
				.map(this::toPronunciationSuggestion)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<CompletedPronunciation> latestCompletedPronunciation(UUID wordInfoRecordId) {
		return pronunciationRepository
				.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(wordInfoRecordId, PronunciationAssetStatus.COMPLETED)
				.map(this::toCompletedPronunciation);
	}

	@Transactional(readOnly = true)
	public Optional<CompletedPhraseImage> latestCompletedPhraseImage(UUID wordInfoRecordId, String normalizedPhrase) {
		return phraseImageRepository
				.findFirstByWordInfoRecordIdAndNormalizedPhraseAndStatusOrderByUpdatedAtDesc(wordInfoRecordId,
						normalizedPhrase, PhraseImageAssetStatus.COMPLETED)
				.map(this::toCompletedPhraseImage);
	}

	private PronunciationSuggestion toPronunciationSuggestion(PronunciationAsset asset) {
		URI videoUrl = null;
		URI fullVideoUrl = null;
		if (asset.getStatus() == PronunciationAssetStatus.COMPLETED && StringUtils.hasText(asset.getVideoObjectKey())) {
			videoUrl = smallVideoUri(asset.getId());
			fullVideoUrl = fullVideoUri(asset.getId());
		}
		return new PronunciationSuggestion(asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getWordInfoRecord().getId(),
				asset.getId(), asset.getStatus().name().toLowerCase(), videoUrl, fullVideoUrl);
	}

	private CompletedPronunciation toCompletedPronunciation(PronunciationAsset asset) {
		return new CompletedPronunciation(asset.getId(), asset.getWordInfoRecord().getId(), asset.getNormalizedPhrase(),
				smallVideoUri(asset.getId()), fullVideoUri(asset.getId()), asset.getUpdatedAt());
	}

	private CompletedPhraseImage toCompletedPhraseImage(PhraseImageAsset asset) {
		return new CompletedPhraseImage(asset.getId(), imageUri(asset.getId()));
	}

	private static URI smallVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video/small");
	}

	private static URI fullVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video");
	}

	private static URI imageUri(UUID id) {
		return URI.create("/api/v1/media/phrase-images/" + id + "/image");
	}

	public record PronunciationSuggestion(
			String word,
			String phrase,
			UUID wordInfoId,
			UUID pronunciationId,
			String status,
			URI videoUrl,
			URI fullVideoUrl) {
	}

	public record CompletedPronunciation(
			UUID id,
			UUID wordInfoId,
			String phrase,
			URI videoUrl,
			URI fullVideoUrl,
			OffsetDateTime updatedAt) {
	}

	public record CompletedPhraseImage(UUID id, URI imageUrl) {
	}

}

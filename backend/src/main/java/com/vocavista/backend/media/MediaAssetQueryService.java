package com.vocavista.backend.media;

import com.vocavista.backend.media.pronunciation.PhraseImageAsset;
import com.vocavista.backend.media.pronunciation.PhraseImageAssetStatus;
import com.vocavista.backend.media.pronunciation.PhraseImageRepository;
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

@Service
@RequiredArgsConstructor
public class MediaAssetQueryService {

	private final PronunciationRepository pronunciationRepository;
	private final PhraseImageRepository phraseImageRepository;
	private final MediaAssetMapper mediaAssetMapper;

	@Transactional(readOnly = true)
	public List<PronunciationSuggestion> findPronunciationSuggestions(String query) {
		return pronunciationRepository.findTop10ByInputWordContainingIgnoreCaseOrderByUpdatedAtDesc(query)
				.stream()
				.map(mediaAssetMapper::toPronunciationSuggestion)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<CompletedPronunciation> latestCompletedPronunciation(UUID vocabularyItemId) {
		return pronunciationRepository
				.findFirstByVocabularyItemIdAndStatusOrderByUpdatedAtDesc(vocabularyItemId, PronunciationAssetStatus.COMPLETED)
				.map(mediaAssetMapper::toCompletedPronunciation);
	}

	@Transactional(readOnly = true)
	public Optional<CompletedPhraseImage> latestCompletedPhraseImage(UUID vocabularyItemId, String phrase) {
		return phraseImageRepository
				.findFirstByVocabularyItemIdAndInputPhraseIgnoreCaseAndStatusOrderByUpdatedAtDesc(vocabularyItemId,
						phrase, PhraseImageAssetStatus.COMPLETED)
				.map(mediaAssetMapper::toCompletedPhraseImage);
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

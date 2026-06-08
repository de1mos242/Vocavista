package com.vocavista.backend.media.pronunciation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhraseImageRepository extends JpaRepository<PhraseImageAsset, UUID> {

	Optional<PhraseImageAsset> findFirstByLanguageAndContentHashAndStatusNotOrderByCreatedAtAsc(
			String language,
			String contentHash,
			PhraseImageAssetStatus status);

	Optional<PhraseImageAsset> findFirstByWordInfoRecordIdAndNormalizedPhraseAndStatusOrderByUpdatedAtDesc(
			UUID wordInfoRecordId,
			String normalizedPhrase,
			PhraseImageAssetStatus status);

}

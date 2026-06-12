package com.vocavista.backend.media.pronunciation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PronunciationRepository extends JpaRepository<PronunciationAsset, UUID> {

	Optional<PronunciationAsset> findByWordInfoRecordIdAndNormalizedPhrase(UUID wordInfoRecordId, String normalizedPhrase);

	Optional<PronunciationAsset> findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(UUID wordInfoRecordId,
			PronunciationAssetStatus status);

	List<PronunciationAsset> findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(String query);

}

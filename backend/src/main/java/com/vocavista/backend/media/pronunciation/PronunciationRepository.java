package com.vocavista.backend.media.pronunciation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PronunciationRepository extends JpaRepository<PronunciationAsset, UUID> {

	@Query("""
			select asset from PronunciationAsset asset
			where asset.vocabularyItem.id = :vocabularyItemId
			  and lower(asset.inputPhrase) = lower(:phrase)
			""")
	Optional<PronunciationAsset> findByVocabularyItemIdAndPhraseIgnoreCase(@Param("vocabularyItemId") UUID vocabularyItemId,
			@Param("phrase") String phrase);

	Optional<PronunciationAsset> findFirstByVocabularyItemIdAndStatusOrderByUpdatedAtDesc(UUID vocabularyItemId,
			PronunciationAssetStatus status);

	List<PronunciationAsset> findTop10ByInputWordContainingIgnoreCaseOrderByUpdatedAtDesc(String query);

}

package com.vocavista.backend.media.pronunciation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhraseImageRepository extends JpaRepository<PhraseImageAsset, UUID> {

	@Query("""
			select asset from PhraseImageAsset asset
			where asset.vocabularyItem.id = :vocabularyItemId
			  and lower(asset.inputPhrase) = lower(:phrase)
			""")
	Optional<PhraseImageAsset> findByVocabularyItemIdAndPhraseIgnoreCase(@Param("vocabularyItemId") UUID vocabularyItemId,
			@Param("phrase") String phrase);

	Optional<PhraseImageAsset> findFirstByVocabularyItemIdAndInputPhraseIgnoreCaseAndStatusOrderByUpdatedAtDesc(
			UUID vocabularyItemId,
			String phrase,
			PhraseImageAssetStatus status);

}

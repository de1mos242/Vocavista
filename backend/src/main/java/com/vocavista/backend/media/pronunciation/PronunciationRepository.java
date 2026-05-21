package com.vocavista.backend.media.pronunciation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PronunciationRepository extends JpaRepository<PronunciationAsset, UUID> {

	Optional<PronunciationAsset> findFirstByLanguageAndContentHashOrderByCreatedAtAsc(String language,
			String contentHash);

}

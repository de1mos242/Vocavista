package com.vocavista.backend.media.pronunciation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PronunciationVideoRepository extends JpaRepository<PronunciationVideoAsset, UUID> {

	Optional<PronunciationVideoAsset> findFirstByLanguageAndContentHashOrderByCreatedAtAsc(String language,
			String contentHash);

}

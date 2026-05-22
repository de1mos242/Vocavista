package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordInfoRepository extends JpaRepository<WordInfoRecord, UUID> {

	Optional<WordInfoRecord> findByNormalizedQuery(String normalizedQuery);

	List<WordInfoRecord> findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc(String query);

}

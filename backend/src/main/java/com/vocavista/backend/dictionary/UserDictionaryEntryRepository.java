package com.vocavista.backend.dictionary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserDictionaryEntryRepository extends JpaRepository<UserDictionaryEntry, UUID> {

	Optional<UserDictionaryEntry> findByUserAccountIdAndNormalizedWord(UUID userAccountId, String normalizedWord);

	Optional<UserDictionaryEntry> findByIdAndUserAccountId(UUID id, UUID userAccountId);

	List<UserDictionaryEntry> findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(UUID userAccountId,
			OffsetDateTime dueAt, Pageable pageable);

	List<UserDictionaryEntry> findByUserAccountIdOrderByDueAtAsc(UUID userAccountId, Pageable pageable);

	List<UserDictionaryEntry> findByUserAccountIdOrderByNormalizedWordAsc(UUID userAccountId);

}

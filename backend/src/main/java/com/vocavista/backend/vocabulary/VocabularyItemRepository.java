package com.vocavista.backend.vocabulary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VocabularyItemRepository extends JpaRepository<VocabularyItem, UUID> {

	@EntityGraph(attributePaths = "translations")
	@Query("""
			select item from VocabularyItem item
			where item.language = :language
			  and lower(item.word) = lower(:word)
			order by item.updatedAt desc
			""")
	List<VocabularyItem> findByLanguageAndWordIgnoreCase(@Param("language") String language, @Param("word") String word);

	@EntityGraph(attributePaths = "translations")
	@Query("""
			select item from VocabularyItem item
			where item.language = :language
			  and lower(item.word) like lower(concat('%', :query, '%'))
			order by item.updatedAt desc
			""")
	List<VocabularyItem> findTop10ByLanguageAndWordContainingIgnoreCase(@Param("language") String language,
			@Param("query") String query, Pageable pageable);

}

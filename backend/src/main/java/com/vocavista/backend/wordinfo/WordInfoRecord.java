package com.vocavista.backend.wordinfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "word_info_records")
@Getter
@Setter
@NoArgsConstructor
public class WordInfoRecord {

	@Id
	private UUID id;

	@Column(name = "normalized_query", nullable = false)
	private String normalizedQuery;

	@Column(name = "normalized_word", nullable = false)
	private String normalizedWord;

	@Column(nullable = false)
	private String language;

	@Column(name = "response_json", nullable = false, columnDefinition = "text")
	private String responseJson;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	static WordInfoRecord create(UUID id, String normalizedQuery, String normalizedWord, String language, String responseJson,
			OffsetDateTime now) {
		WordInfoRecord record = new WordInfoRecord();
		record.id = id;
		record.normalizedQuery = normalizedQuery;
		record.normalizedWord = normalizedWord;
		record.language = language;
		record.responseJson = responseJson;
		record.createdAt = now;
		record.updatedAt = now;
		return record;
	}

}

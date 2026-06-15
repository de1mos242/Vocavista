package com.vocavista.backend.vocabulary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vocabulary_item_translations")
@Getter
@Setter
@NoArgsConstructor
public class VocabularyItemTranslation {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vocabulary_item_id", nullable = false)
	private VocabularyItem vocabularyItem;

	@Column(nullable = false)
	private String language;

	@Column(name = "word_translation", nullable = false)
	private String wordTranslation;

	@Column(name = "phrase_translation", nullable = false)
	private String phraseTranslation;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

}

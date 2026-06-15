package com.vocavista.backend.vocabulary;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vocabulary_items")
@Getter
@Setter
@NoArgsConstructor
public class VocabularyItem {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String language;

	@Column(nullable = false)
	private String word;

	@Column(nullable = false)
	private String phrase;

	@Column(name = "part_of_speech", nullable = false)
	private String partOfSpeech;

	private String gender;

	private String plural;

	@Column(nullable = false)
	private String frequency;

	@Column(name = "is_compound", nullable = false)
	private boolean compound;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@OneToMany(mappedBy = "vocabularyItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<VocabularyItemTranslation> translations = new ArrayList<>();

}

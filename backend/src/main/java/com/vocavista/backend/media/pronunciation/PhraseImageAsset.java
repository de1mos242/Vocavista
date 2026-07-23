package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.vocabulary.VocabularyItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "phrase_image_assets")
@Getter
@Setter
@NoArgsConstructor
public class PhraseImageAsset {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vocabulary_item_id", nullable = false)
	private VocabularyItem vocabularyItem;

	@Column(name = "input_word", nullable = false)
	private String inputWord;

	@Column(name = "input_phrase", nullable = false)
	private String inputPhrase;

	@Column(nullable = false)
	private String language;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PhraseImageAssetStatus status;

	@Column(name = "image_object_key")
	private String imageObjectKey;

	@Column(name = "image_candidate_count", nullable = false)
	private int imageCandidateCount;

	@Column(name = "image_provider")
	private String imageProvider;

	@Column(name = "image_model")
	private String imageModel;

	@Column(name = "prompt_version", nullable = false)
	private String promptVersion;

	@Column(name = "prompt_text")
	private String promptText;

	@Column(name = "scene_plan")
	private String scenePlan;

	@Column(name = "quality_metadata")
	private String qualityMetadata;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "completed_at")
	private OffsetDateTime completedAt;

	static PhraseImageAsset queued(
			VocabularyItem vocabularyItem,
			String inputWord,
			String inputPhrase,
			String language,
			String promptVersion,
			OffsetDateTime now) {
		PhraseImageAsset asset = new PhraseImageAsset();
		asset.id = UUID.randomUUID();
		asset.vocabularyItem = vocabularyItem;
		asset.inputWord = inputWord;
		asset.inputPhrase = inputPhrase;
		asset.language = language;
		asset.promptVersion = promptVersion;
		asset.status = PhraseImageAssetStatus.QUEUED;
		asset.imageCandidateCount = 0;
		asset.createdAt = now;
		asset.updatedAt = now;
		return asset;
	}

}

package com.vocavista.backend.media.pronunciation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pronunciation_video_assets")
@Getter
@Setter
@NoArgsConstructor
class PronunciationVideoAsset {

	@Id
	private UUID id;

	@Column(name = "normalized_word", nullable = false)
	private String normalizedWord;

	@Column(name = "normalized_phrase", nullable = false)
	private String normalizedPhrase;

	@Column(name = "input_word", nullable = false)
	private String inputWord;

	@Column(name = "input_phrase", nullable = false)
	private String inputPhrase;

	@Column(nullable = false)
	private String language;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PronunciationVideoAssetStatus status;

	@Column(name = "audio_object_key")
	private String audioObjectKey;

	@Column(name = "video_object_key")
	private String videoObjectKey;

	@Column(name = "audio_provider")
	private String audioProvider;

	@Column(name = "audio_model")
	private String audioModel;

	@Column(name = "video_provider")
	private String videoProvider;

	@Column(name = "video_model")
	private String videoModel;

	@Column(name = "content_hash", nullable = false)
	private String contentHash;

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

	static PronunciationVideoAsset queued(
			String inputWord,
			String inputPhrase,
			String normalizedWord,
			String normalizedPhrase,
			String language,
			String contentHash,
			OffsetDateTime now) {
		PronunciationVideoAsset asset = new PronunciationVideoAsset();
		asset.id = UUID.randomUUID();
		asset.inputWord = inputWord;
		asset.inputPhrase = inputPhrase;
		asset.normalizedWord = normalizedWord;
		asset.normalizedPhrase = normalizedPhrase;
		asset.language = language;
		asset.contentHash = contentHash;
		asset.status = PronunciationVideoAssetStatus.QUEUED;
		asset.createdAt = now;
		asset.updatedAt = now;
		return asset;
	}

}

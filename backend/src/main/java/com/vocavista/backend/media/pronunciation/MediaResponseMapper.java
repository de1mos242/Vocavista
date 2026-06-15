package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PhraseImageResponse;
import com.vocavista.backend.api.model.PhraseImageStatus;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
interface MediaResponseMapper {

	@Mapping(target = "wordInfoId", source = "wordInfoRecord.id")
	@Mapping(target = "status", source = "status")
	@Mapping(target = "videoUrl", source = ".", qualifiedByName = "smallVideoUriIfPresent")
	@Mapping(target = "fullVideoUrl", source = ".", qualifiedByName = "fullVideoUriIfPresent")
	@Mapping(target = "errorCode", source = ".", qualifiedByName = "pronunciationErrorCode")
	@Mapping(target = "errorMessage", source = ".", qualifiedByName = "pronunciationErrorMessage")
	PronunciationResponse toResponse(PronunciationAsset asset);

	@Mapping(target = "wordInfoId", source = "wordInfoRecord.id")
	@Mapping(target = "status", source = "status")
	@Mapping(target = "word", source = "normalizedWord")
	@Mapping(target = "phrase", source = "normalizedPhrase")
	@Mapping(target = "imageUrl", source = ".", qualifiedByName = "imageUriIfPresent")
	@Mapping(target = "candidateImageUrls", source = ".", qualifiedByName = "candidateUrisIfPresent")
	@Mapping(target = "errorCode", source = ".", qualifiedByName = "phraseImageErrorCode")
	@Mapping(target = "errorMessage", source = ".", qualifiedByName = "phraseImageErrorMessage")
	PhraseImageResponse toResponse(PhraseImageAsset asset);

	default PronunciationStatus mapPronunciationStatus(PronunciationAssetStatus status) {
		return status == null ? null : PronunciationStatus.fromValue(status.name().toLowerCase());
	}

	default PhraseImageStatus mapPhraseImageStatus(PhraseImageAssetStatus status) {
		return status == null ? null : PhraseImageStatus.fromValue(status.name().toLowerCase());
	}

	@Named("smallVideoUriIfPresent")
	default URI smallVideoUriIfPresent(PronunciationAsset asset) {
		return StringUtils.hasText(asset.getVideoObjectKey()) ? smallVideoUri(asset.getId()) : null;
	}

	@Named("fullVideoUriIfPresent")
	default URI fullVideoUriIfPresent(PronunciationAsset asset) {
		return StringUtils.hasText(asset.getVideoObjectKey()) ? fullVideoUri(asset.getId()) : null;
	}

	@Named("imageUriIfPresent")
	default URI imageUriIfPresent(PhraseImageAsset asset) {
		return StringUtils.hasText(asset.getImageObjectKey()) ? imageUri(asset.getId()) : null;
	}

	@Named("candidateUrisIfPresent")
	default List<URI> candidateUrisIfPresent(PhraseImageAsset asset) {
		if (asset.getStatus() != PhraseImageAssetStatus.AWAITING_SELECTION || asset.getImageCandidateCount() <= 0) {
			return List.of();
		}
		return IntStream.range(0, asset.getImageCandidateCount())
				.mapToObj(index -> candidateImageUri(asset.getId(), index))
				.toList();
	}

	@Named("pronunciationErrorCode")
	default String pronunciationErrorCode(PronunciationAsset asset) {
		return asset.getStatus() == PronunciationAssetStatus.FAILED ? asset.getErrorCode() : null;
	}

	@Named("pronunciationErrorMessage")
	default String pronunciationErrorMessage(PronunciationAsset asset) {
		return asset.getStatus() == PronunciationAssetStatus.FAILED ? asset.getErrorMessage() : null;
	}

	@Named("phraseImageErrorCode")
	default String phraseImageErrorCode(PhraseImageAsset asset) {
		return asset.getStatus() == PhraseImageAssetStatus.FAILED ? asset.getErrorCode() : null;
	}

	@Named("phraseImageErrorMessage")
	default String phraseImageErrorMessage(PhraseImageAsset asset) {
		return asset.getStatus() == PhraseImageAssetStatus.FAILED ? asset.getErrorMessage() : null;
	}

	private static URI smallVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video/small");
	}

	private static URI fullVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video");
	}

	private static URI imageUri(UUID id) {
		return URI.create("/api/v1/media/phrase-images/" + id + "/image");
	}

	private static URI candidateImageUri(UUID id, int index) {
		return URI.create("/api/v1/media/phrase-images/" + id + "/candidates/" + index + "/image");
	}

}

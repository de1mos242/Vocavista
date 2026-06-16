package com.vocavista.backend.media;

import com.vocavista.backend.media.MediaAssetQueryService.CompletedPhraseImage;
import com.vocavista.backend.media.MediaAssetQueryService.CompletedPronunciation;
import com.vocavista.backend.media.MediaAssetQueryService.PronunciationSuggestion;
import com.vocavista.backend.media.pronunciation.PhraseImageAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import java.net.URI;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
interface MediaAssetMapper {

	@Mapping(target = "word", source = "inputWord")
	@Mapping(target = "phrase", source = "inputPhrase")
	@Mapping(target = "wordInfoId", source = "vocabularyItem.id")
	@Mapping(target = "pronunciationId", source = "id")
	@Mapping(target = "status", source = "status", qualifiedByName = "pronunciationStatus")
	@Mapping(target = "videoUrl", source = ".", qualifiedByName = "smallVideoUriIfCompleted")
	@Mapping(target = "fullVideoUrl", source = ".", qualifiedByName = "fullVideoUriIfCompleted")
	PronunciationSuggestion toPronunciationSuggestion(PronunciationAsset asset);

	@Mapping(target = "wordInfoId", source = "vocabularyItem.id")
	@Mapping(target = "phrase", source = "inputPhrase")
	@Mapping(target = "videoUrl", source = "id", qualifiedByName = "smallVideoUri")
	@Mapping(target = "fullVideoUrl", source = "id", qualifiedByName = "fullVideoUri")
	CompletedPronunciation toCompletedPronunciation(PronunciationAsset asset);

	@Mapping(target = "imageUrl", source = "id", qualifiedByName = "imageUri")
	CompletedPhraseImage toCompletedPhraseImage(PhraseImageAsset asset);

	@Named("pronunciationStatus")
	default String pronunciationStatus(PronunciationAssetStatus status) {
		return status == null ? null : status.name().toLowerCase();
	}

	@Named("smallVideoUriIfCompleted")
	default URI smallVideoUriIfCompleted(PronunciationAsset asset) {
		return hasCompletedVideo(asset) ? smallVideoUri(asset.getId()) : null;
	}

	@Named("fullVideoUriIfCompleted")
	default URI fullVideoUriIfCompleted(PronunciationAsset asset) {
		return hasCompletedVideo(asset) ? fullVideoUri(asset.getId()) : null;
	}

	@Named("smallVideoUri")
	default URI smallVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video/small");
	}

	@Named("fullVideoUri")
	default URI fullVideoUri(UUID id) {
		return URI.create("/api/v1/media/pronunciations/" + id + "/video");
	}

	@Named("imageUri")
	default URI imageUri(UUID id) {
		return URI.create("/api/v1/media/phrase-images/" + id + "/image");
	}

	private static boolean hasCompletedVideo(PronunciationAsset asset) {
		return asset != null
				&& asset.getStatus() == PronunciationAssetStatus.COMPLETED
				&& StringUtils.hasText(asset.getVideoObjectKey());
	}

}

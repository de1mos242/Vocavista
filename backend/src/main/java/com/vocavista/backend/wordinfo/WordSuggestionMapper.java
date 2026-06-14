package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.api.model.WordSuggestion;
import com.vocavista.backend.media.MediaAssetQueryService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface WordSuggestionMapper {

	@Mapping(target = "word", source = "normalizedWord")
	@Mapping(target = "source", expression = "java(com.vocavista.backend.api.model.WordSuggestion.SourceEnum.WORD_INFO)")
	@Mapping(target = "wordInfoId", source = "id")
	@Mapping(target = "phrase", ignore = true)
	@Mapping(target = "pronunciationId", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "videoUrl", ignore = true)
	@Mapping(target = "fullVideoUrl", ignore = true)
	WordSuggestion toWordInfoSuggestion(WordInfoRecord record);

	@Mapping(target = "source", expression = "java(com.vocavista.backend.api.model.WordSuggestion.SourceEnum.PRONUNCIATION)")
	@Mapping(target = "status", source = "status")
	WordSuggestion toPronunciationSuggestion(MediaAssetQueryService.PronunciationSuggestion pronunciation);

	default PronunciationStatus mapStatus(String status) {
		return status == null ? null : PronunciationStatus.fromValue(status);
	}

}

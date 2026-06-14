package com.vocavista.backend.dictionary;

import com.vocavista.backend.api.model.DictionaryReviewItem;
import com.vocavista.backend.api.model.DictionaryVideoManifestItem;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.media.MediaAssetQueryService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface DictionaryMapper {

	@Mapping(target = "entryId", source = "entry.id")
	@Mapping(target = "wordInfoId", source = "entry.wordInfoRecord.id")
	@Mapping(target = "normalizedWord", source = "entry.normalizedWord")
	@Mapping(target = "expectedAnswer", source = "expectedAnswer")
	@Mapping(target = "translations", source = "wordInfo.translations")
	@Mapping(target = "partOfSpeech", source = "wordInfo.partOfSpeech")
	@Mapping(target = "dueAt", source = "entry.dueAt")
	@Mapping(target = "article", source = "wordInfo.article")
	@Mapping(target = "pronunciationAssetId", ignore = true)
	@Mapping(target = "phrase", ignore = true)
	@Mapping(target = "phraseImageId", ignore = true)
	@Mapping(target = "phraseImageUrl", ignore = true)
	DictionaryReviewItem toReviewItem(UserDictionaryEntry entry, WordInfoResponse wordInfo, String expectedAnswer);

	@Mapping(target = "pronunciationAssetId", source = "id")
	DictionaryVideoManifestItem toVideoManifestItem(MediaAssetQueryService.CompletedPronunciation pronunciation);

}

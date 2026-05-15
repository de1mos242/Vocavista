package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.CompoundPart;
import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.GermanArticle;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordExample;
import com.vocavista.backend.api.model.WordFrequency;
import com.vocavista.backend.api.model.WordInfoResponse;
import org.mapstruct.Mapper;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
interface WordInfoMapper {

	WordInfoResponse toApiResponse(ProviderWordInfo wordInfo);

	CompoundPart toApiCompoundPart(ProviderWordInfo.CompoundPart compoundPart);

	WordExample toApiExample(ProviderWordInfo.WordExample example);

	default String mapString(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	default WordInfoResponse.LanguageEnum mapLanguage(String value) {
		return WordInfoResponse.LanguageEnum.fromValue(value.trim());
	}

	default PartOfSpeech mapPartOfSpeech(String value) {
		return PartOfSpeech.fromValue(value.trim());
	}

	default WordFrequency mapWordFrequency(String value) {
		return WordFrequency.fromValue(value.trim());
	}

	default Gender mapGender(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return Gender.fromValue(value.trim());
	}

	default GermanArticle mapGermanArticle(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return GermanArticle.fromValue(value.trim());
	}

}

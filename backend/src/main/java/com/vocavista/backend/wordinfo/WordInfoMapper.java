package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.CompoundPart;
import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.GermanArticle;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordExample;
import com.vocavista.backend.api.model.WordFrequency;
import com.vocavista.backend.api.model.WordInfoResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	default Map<String, List<String>> mapLocalizedText(ProviderWordInfo.LocalizedText value) {
		return value == null ? null : Map.of("en", value.en(), "ru", value.ru());
	}

	default WordInfoResponse.LanguageEnum mapLanguage(ProviderWordInfo.Language value) {
		return value == null ? null : WordInfoResponse.LanguageEnum.fromValue(value.name());
	}

	default PartOfSpeech mapPartOfSpeech(ProviderWordInfo.ProviderPartOfSpeech value) {
		return value == null ? null : PartOfSpeech.fromValue(value.name());
	}

	default WordFrequency mapWordFrequency(ProviderWordInfo.ProviderFrequency value) {
		return value == null ? null : WordFrequency.fromValue(value.name());
	}

	default Gender mapGender(Optional<ProviderWordInfo.ProviderGender> value) {
		return value == null || value.isEmpty() ? null : Gender.fromValue(value.get().name());
	}

	default GermanArticle mapGermanArticle(Optional<ProviderWordInfo.ProviderArticle> value) {
		return value == null || value.isEmpty() ? null : GermanArticle.fromValue(value.get().name());
	}

	default String mapOptionalString(Optional<String> value) {
		return value == null || value.isEmpty() ? null : mapString(value.get());
	}

}

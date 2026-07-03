package com.vocavista.backend.vocabulary;

import com.vocavista.backend.api.model.VocabularyItemDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
public abstract class VocabularyItemMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "word", source = "dto.word", qualifiedByName = "trimAndCollapse")
	@Mapping(target = "phrase", source = "dto.phrase", qualifiedByName = "trimAndCollapse")
	@Mapping(target = "language", source = "dto.language", qualifiedByName = "languageCode")
	@Mapping(target = "partOfSpeech", expression = "java(dto.getPartOfSpeech().getValue())")
	@Mapping(target = "gender", expression = "java(dto.getGender() == null ? null : dto.getGender().getValue())")
	@Mapping(target = "plural", source = "dto.plural", qualifiedByName = "trimOptional")
	@Mapping(target = "frequency", expression = "java(dto.getFrequency().getValue())")
	@Mapping(target = "compound", source = "dto.isCompound")
	@Mapping(target = "createdAt", expression = "java(now)")
	@Mapping(target = "updatedAt", expression = "java(now)")
	public abstract VocabularyItem toEntity(VocabularyItemDto dto, UUID id, @Context OffsetDateTime now);

	@Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
	@Mapping(target = "vocabularyItem", ignore = true)
	@Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
	@Mapping(target = "wordTranslation", source = "wordTranslation", qualifiedByName = "trimAndCollapse")
	@Mapping(target = "phraseTranslation", source = "phraseTranslation", qualifiedByName = "trimAndCollapse")
	@Mapping(target = "createdAt", expression = "java(now)")
	@Mapping(target = "updatedAt", expression = "java(now)")
	abstract VocabularyItemTranslation toEntity(
			com.vocavista.backend.api.model.VocabularyItemTranslation dto,
			@Context OffsetDateTime now);

	@AfterMapping
	void linkTranslations(@MappingTarget VocabularyItem item) {
		if (item.getTranslations() != null) {
			item.getTranslations().forEach(translation -> translation.setVocabularyItem(item));
		}
	}

	@Named("trimAndCollapse")
	String trimAndCollapse(String value) {
		return VocabularyText.optional(value);
	}

	@Named("trimOptional")
	String trimOptional(String value) {
		String trimmed = VocabularyText.optional(value);
		return StringUtils.hasText(trimmed) ? trimmed : null;
	}

	@Named("languageCode")
	String languageCode(String value) {
		return VocabularyText.languageCode(value);
	}

}

package com.vocavista.backend.wordinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WordInfoMetadataReader {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public PronunciationMetadata pronunciationMetadata(WordInfoRecord record) {
		try {
			String responseJson = record.getResponseJson();
			if (responseJson == null || responseJson.isBlank()) {
				return PronunciationMetadata.defaultMetadata();
			}
			WordInfoResponse wordInfo = objectMapper.readValue(responseJson, WordInfoResponse.class);
			String article = wordInfo.getPartOfSpeech() == PartOfSpeech.NOUN && wordInfo.getArticle() != null
					? wordInfo.getArticle().getValue()
					: null;
			return new PronunciationMetadata(article, speakerDescription(wordInfo.getGender()));
		}
		catch (JsonProcessingException | IllegalArgumentException ex) {
			log.warn("Could not read word info metadata for pronunciation script", ex);
			return PronunciationMetadata.defaultMetadata();
		}
	}

	private static String speakerDescription(Gender gender) {
		return switch (gender) {
			case MASCULINE -> "male german adult speaker";
			case FEMININE -> "female german adult speaker";
			case NEUTER -> "young german adult woman";
			case null -> "young german adult woman";
		};
	}

	public record PronunciationMetadata(String article, String speakerDescription) {

		static PronunciationMetadata defaultMetadata() {
			return new PronunciationMetadata(null, "young adult woman");
		}

	}

}

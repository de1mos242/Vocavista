package com.vocavista.backend.wordinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WordInfoArticleReader {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public String nounArticle(WordInfoRecord record) {
		try {
			String responseJson = record.getResponseJson();
			if (responseJson == null || responseJson.isBlank()) {
				return null;
			}
			WordInfoResponse wordInfo = objectMapper.readValue(responseJson, WordInfoResponse.class);
			return wordInfo.getPartOfSpeech() == PartOfSpeech.NOUN && wordInfo.getArticle() != null
					? wordInfo.getArticle().getValue()
					: null;
		}
		catch (JsonProcessingException | IllegalArgumentException ex) {
			log.warn("Could not read stored word article", ex);
			return null;
		}
	}

}

package com.vocavista.backend.wordinfo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@WebMvcTest(WordInfoController.class)
@Import({ WordInfoService.class, WordSuggestionService.class, ProviderWordInfoValidator.class, WordInfoMapperImpl.class,
		WordInfoErrorHandler.class })
class WordInfoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiWordInfoProvider aiWordInfoProvider;

	@MockitoBean
	private WordInfoRepository wordInfoRepository;

	@MockitoBean
	private PronunciationRepository pronunciationRepository;

	@BeforeEach
	void setUp() {
		when(wordInfoRepository.findByNormalizedQuery(anyString())).thenReturn(Optional.empty());
	}

	@Test
	void returnsWordInfoForValidRequest() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenReturn(SampleWordInfos.nounInfo());

		mockMvc.perform(get("/api/v1/words/info").param("word", " Hausaufgabe "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.normalizedWord").value("Hausaufgabe"))
				.andExpect(jsonPath("$.partOfSpeech").value("noun"))
				.andExpect(jsonPath("$.examples.length()").value(3));

		verify(aiWordInfoProvider).generate("Hausaufgabe");
	}

	@Test
	void returnsWordSuggestionsForValidQuery() throws Exception {
		WordInfoRecord record = WordInfoRecord.create("hausaufgabe", "Hausaufgabe", "de", "{}", OffsetDateTime.now());
		when(wordInfoRepository.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc("haus"))
				.thenReturn(List.of(record));
		when(pronunciationRepository.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc("haus"))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/v1/words/suggestions").param("query", "haus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].word").value("Hausaufgabe"))
				.andExpect(jsonPath("$.items[0].source").value("word_info"));
	}

	@Test
	void rejectsMissingWord() throws Exception {
		mockMvc.perform(get("/api/v1/words/info"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	void rejectsBlankWordAfterTrimming() throws Exception {
		mockMvc.perform(get("/api/v1/words/info").param("word", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	void rejectsTooLongWord() throws Exception {
		mockMvc.perform(get("/api/v1/words/info").param("word", "a".repeat(81)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	void mapsProviderBadGatewayError() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenThrow(new AiProviderBadGatewayException("bad"));

		mockMvc.perform(get("/api/v1/words/info").param("word", "Hausaufgabe"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("ai_provider_error"));
	}

	@Test
	void mapsProviderUnavailableError() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenThrow(new AiProviderUnavailableException("unavailable"));

		mockMvc.perform(get("/api/v1/words/info").param("word", "Hausaufgabe"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("ai_provider_unavailable"));
	}

}

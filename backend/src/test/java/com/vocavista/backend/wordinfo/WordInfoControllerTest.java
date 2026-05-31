package com.vocavista.backend.wordinfo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.auth.AuthErrorHandler;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.FunctionalAccessInterceptor;
import com.vocavista.backend.auth.FunctionalAccessWebConfig;
import com.vocavista.backend.auth.GoogleOidcUserService;
import com.vocavista.backend.auth.UserAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WebMvcTest(value = WordInfoController.class, excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class)
@Import({ WordInfoService.class, WordSuggestionService.class, ProviderWordInfoValidator.class, WordInfoMapperImpl.class,
		WordInfoErrorHandler.class, AuthErrorHandler.class, FunctionalAccessInterceptor.class, FunctionalAccessWebConfig.class })
class WordInfoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiWordInfoProvider aiWordInfoProvider;

	@MockitoBean
	private WordInfoRepository wordInfoRepository;

	@MockitoBean
	private PronunciationRepository pronunciationRepository;

	@MockitoBean
	private GoogleOidcUserService googleOidcUserService;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private UserAccessService userAccessService;

	@BeforeEach
	void setUp() {
		when(wordInfoRepository.findByNormalizedQuery(anyString())).thenReturn(Optional.empty());
	}

	@Test
	@WithMockUser
	void returnsWordInfoForValidRequest() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenReturn(new AiWordInfoResult(SampleWordInfos.nounInfo(), "{}"));

		mockMvc.perform(get("/api/v1/words/info").param("word", " Hausaufgabe "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.normalizedWord").value("Hausaufgabe"))
				.andExpect(jsonPath("$.partOfSpeech").value("noun"))
				.andExpect(jsonPath("$.examples.length()").value(3));

		verify(aiWordInfoProvider).generate("Hausaufgabe");
	}

	@Test
	@WithMockUser
	void rejectsWordInfoWhenUserCannotUseFunctionalFeatures() throws Exception {
		doThrow(new AccessDeniedException("Account is not approved to use app features"))
				.when(userAccessService).requireFunctionalAccess(any());

		mockMvc.perform(get("/api/v1/words/info").param("word", "Hausaufgabe"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	@WithMockUser
	void returnsWordSuggestionsForValidQuery() throws Exception {
		UUID wordInfoId = UUID.randomUUID();
		WordInfoRecord record = WordInfoRecord.create(wordInfoId, "hausaufgabe", "Hausaufgabe", "de", "{}",
				OffsetDateTime.now());
		when(wordInfoRepository.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc("haus"))
				.thenReturn(List.of(record));
		when(pronunciationRepository.findTop10ByNormalizedWordContainingIgnoreCaseOrderByUpdatedAtDesc("haus"))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/v1/words/suggestions").param("query", "haus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].word").value("Hausaufgabe"))
				.andExpect(jsonPath("$.items[0].wordInfoId").value(wordInfoId.toString()))
				.andExpect(jsonPath("$.items[0].source").value("word_info"));
	}

	@Test
	@WithMockUser
	void rejectsMissingWord() throws Exception {
		mockMvc.perform(get("/api/v1/words/info"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	@WithMockUser
	void rejectsBlankWordAfterTrimming() throws Exception {
		mockMvc.perform(get("/api/v1/words/info").param("word", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	@WithMockUser
	void rejectsTooLongWord() throws Exception {
		mockMvc.perform(get("/api/v1/words/info").param("word", "a".repeat(81)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	@WithMockUser
	void mapsProviderBadGatewayError() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenThrow(new AiProviderBadGatewayException("bad"));

		mockMvc.perform(get("/api/v1/words/info").param("word", "Hausaufgabe"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("ai_provider_error"));
	}

	@Test
	@WithMockUser
	void mapsProviderUnavailableError() throws Exception {
		when(aiWordInfoProvider.generate(anyString())).thenThrow(new AiProviderUnavailableException("unavailable"));

		mockMvc.perform(get("/api/v1/words/info").param("word", "Hausaufgabe"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("ai_provider_unavailable"));
	}

}

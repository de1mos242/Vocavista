package com.vocavista.backend.dictionary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.api.model.DictionaryReviewResponse;
import com.vocavista.backend.api.model.DictionaryReviewSubmitResponse;
import com.vocavista.backend.auth.AuthErrorHandler;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.FunctionalAccessInterceptor;
import com.vocavista.backend.auth.FunctionalAccessWebConfig;
import com.vocavista.backend.auth.GoogleOidcUserService;
import com.vocavista.backend.auth.UserAccessService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = DictionaryController.class, excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class)
@Import({ DictionaryErrorHandler.class, AuthErrorHandler.class, FunctionalAccessInterceptor.class, FunctionalAccessWebConfig.class })
class DictionaryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDictionaryService userDictionaryService;

	@MockitoBean
	private GoogleOidcUserService googleOidcUserService;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private UserAccessService userAccessService;

	@Test
	@WithMockUser
	void returnsReviewItems() throws Exception {
		when(userDictionaryService.getReviewItems(10, true)).thenReturn(new DictionaryReviewResponse(List.of()));

		mockMvc.perform(get("/api/v1/dictionary/review")
				.param("limit", "10")
				.param("includeUpcoming", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());

		verify(userDictionaryService).getReviewItems(10, true);
	}

	@Test
	@WithMockUser
	void rejectsReviewWhenUserCannotUseFunctionalFeatures() throws Exception {
		doThrow(new AccessDeniedException("Account is not approved to use app features"))
				.when(userAccessService).requireFunctionalAccess(any());

		mockMvc.perform(get("/api/v1/dictionary/review"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	@WithMockUser
	void submitsReviewResult() throws Exception {
		UUID entryId = UUID.randomUUID();
		when(userDictionaryService.submitReview(any(), any()))
				.thenReturn(new DictionaryReviewSubmitResponse(entryId, true, "die Hausaufgabe", OffsetDateTime.now(), 1, 1, 1, 0));

		mockMvc.perform(post("/api/v1/dictionary/review/{entryId}", entryId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"correct\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entryId").value(entryId.toString()))
				.andExpect(jsonPath("$.correct").value(true))
				.andExpect(jsonPath("$.expectedAnswer").value("die Hausaufgabe"));
	}

}

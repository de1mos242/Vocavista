package com.vocavista.backend.media.pronunciation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.api.model.PhraseImageResponse;
import com.vocavista.backend.api.model.PhraseImageStatus;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.auth.AuthErrorHandler;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.FunctionalAccessInterceptor;
import com.vocavista.backend.auth.FunctionalAccessWebConfig;
import com.vocavista.backend.auth.GoogleOidcUserService;
import com.vocavista.backend.auth.UserAccessService;
import java.net.URI;
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

@WebMvcTest(value = PronunciationController.class, excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class)
@Import({ MediaErrorHandler.class, AuthErrorHandler.class, FunctionalAccessInterceptor.class, FunctionalAccessWebConfig.class })
class PronunciationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PronunciationService pronunciationService;

	@MockitoBean
	private PhraseImageService phraseImageService;

	@MockitoBean
	private GoogleOidcUserService googleOidcUserService;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private UserAccessService userAccessService;

	@Test
	@WithMockUser
	void queuesPronunciationForValidRequest() throws Exception {
		UUID id = UUID.randomUUID();
		UUID wordInfoId = UUID.randomUUID();
		when(pronunciationService.create(any()))
				.thenReturn(new PronunciationResponse(id, wordInfoId, PronunciationStatus.QUEUED));

		mockMvc.perform(post("/api/v1/media/pronunciations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "wordInfoId": "%s",
						  "word": "Hausaufgabe",
						  "phrase": "Ich mache meine Hausaufgabe nach dem Abendessen.",
						  "language": "de"
						}
						""".formatted(wordInfoId)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.wordInfoId").value(wordInfoId.toString()))
				.andExpect(jsonPath("$.status").value("queued"));

		verify(pronunciationService).create(any());
	}

	@Test
	@WithMockUser
	void rejectsPronunciationGenerationWhenUserCannotUseFunctionalFeatures() throws Exception {
		UUID wordInfoId = UUID.randomUUID();
		doThrow(new AccessDeniedException("Account is not approved to use app features"))
				.when(userAccessService).requireFunctionalAccess(any());

		mockMvc.perform(post("/api/v1/media/pronunciations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "wordInfoId": "%s",
						  "word": "Hausaufgabe",
						  "phrase": "Ich mache meine Hausaufgabe nach dem Abendessen.",
						  "language": "de"
						}
						""".formatted(wordInfoId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	@WithMockUser
	void returnsCompletedPronunciationStatus() throws Exception {
		UUID id = UUID.randomUUID();
		UUID wordInfoId = UUID.randomUUID();
		PronunciationResponse response = new PronunciationResponse(id, wordInfoId, PronunciationStatus.COMPLETED)
				.videoUrl(URI.create("/api/v1/media/pronunciations/%s/video/small".formatted(id)))
				.fullVideoUrl(URI.create("/api/v1/media/pronunciations/%s/video".formatted(id)));
		when(pronunciationService.get(id)).thenReturn(response);

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.wordInfoId").value(wordInfoId.toString()))
				.andExpect(jsonPath("$.status").value("completed"))
				.andExpect(jsonPath("$.videoUrl").exists())
				.andExpect(jsonPath("$.fullVideoUrl").exists());
	}

	@Test
	@WithMockUser
	void returnsGeneratedVideoBytes() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.getVideo(id)).thenReturn(new StoredMedia("video/mp4", "video".getBytes()));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}/video", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType("video/mp4"))
				.andExpect(content().bytes("video".getBytes()));
	}

	@Test
	@WithMockUser
	void returnsSmallGeneratedVideoBytes() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.getSmallVideo(id)).thenReturn(new StoredMedia("video/mp4", "small".getBytes()));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}/video/small", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType("video/mp4"))
				.andExpect(content().bytes("small".getBytes()));
	}

	@Test
	@WithMockUser
	void returnsPhraseImageCandidateBytes() throws Exception {
		UUID id = UUID.randomUUID();
		when(phraseImageService.getCandidateImage(id, 1)).thenReturn(new StoredMedia("image/png", "image".getBytes()));

		mockMvc.perform(get("/api/v1/media/phrase-images/{id}/candidates/{candidateIndex}/image", id, 1))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes("image".getBytes()));
	}

	@Test
	@WithMockUser
	void selectsPhraseImageCandidate() throws Exception {
		UUID id = UUID.randomUUID();
		UUID wordInfoId = UUID.randomUUID();
		when(phraseImageService.selectCandidate(id, 1))
				.thenReturn(new PhraseImageResponse(id, wordInfoId, PhraseImageStatus.COMPLETED)
						.imageUrl(URI.create("/api/v1/media/phrase-images/%s/image".formatted(id))));

		mockMvc.perform(post("/api/v1/media/phrase-images/{id}/candidates/{candidateIndex}/select", id, 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.status").value("completed"))
				.andExpect(jsonPath("$.imageUrl").exists());
	}

	@Test
	@WithMockUser
	void rejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/api/v1/media/pronunciations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "phrase": "Ich mache meine Hausaufgabe nach dem Abendessen.",
						  "language": "de"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	@WithMockUser
	void mapsMissingAssetToNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.get(id))
				.thenThrow(new PronunciationNotFoundException("Pronunciation asset was not found"));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("not_found"));
	}

}

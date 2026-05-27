package com.vocavista.backend.media.pronunciation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PronunciationController.class)
@Import(MediaErrorHandler.class)
class PronunciationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PronunciationService pronunciationService;

	@Test
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
	void returnsCompletedPronunciationStatus() throws Exception {
		UUID id = UUID.randomUUID();
		UUID wordInfoId = UUID.randomUUID();
		PronunciationResponse response = new PronunciationResponse(id, wordInfoId, PronunciationStatus.COMPLETED)
				.videoUrl(URI.create("/api/v1/media/pronunciations/%s/video".formatted(id)))
				.renderMode("veo-video");
		when(pronunciationService.get(id)).thenReturn(response);

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.wordInfoId").value(wordInfoId.toString()))
				.andExpect(jsonPath("$.status").value("completed"))
				.andExpect(jsonPath("$.videoUrl").exists())
				.andExpect(jsonPath("$.renderMode").value("veo-video"));
	}

	@Test
	void returnsGeneratedAudioBytes() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.getAudio(id)).thenReturn(new StoredMedia("audio/mpeg", "audio".getBytes()));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}/audio", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType("audio/mpeg"))
				.andExpect(content().bytes("audio".getBytes()));
	}

	@Test
	void returnsGeneratedVideoBytes() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.getVideo(id)).thenReturn(new StoredMedia("video/mp4", "video".getBytes()));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}/video", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType("video/mp4"))
				.andExpect(content().bytes("video".getBytes()));
	}

	@Test
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
	void mapsMissingAssetToNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationService.get(id))
				.thenThrow(new PronunciationNotFoundException("Pronunciation asset was not found"));

		mockMvc.perform(get("/api/v1/media/pronunciations/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("not_found"));
	}

}

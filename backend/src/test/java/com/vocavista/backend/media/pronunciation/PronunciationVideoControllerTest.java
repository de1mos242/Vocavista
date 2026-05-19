package com.vocavista.backend.media.pronunciation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.api.model.PronunciationVideoResponse;
import com.vocavista.backend.api.model.PronunciationVideoStatus;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PronunciationVideoController.class)
@Import(MediaErrorHandler.class)
class PronunciationVideoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PronunciationVideoService pronunciationVideoService;

	@Test
	void queuesPronunciationVideoForValidRequest() throws Exception {
		UUID id = UUID.randomUUID();
		when(pronunciationVideoService.create(any()))
				.thenReturn(new PronunciationVideoResponse(id, PronunciationVideoStatus.QUEUED));

		mockMvc.perform(post("/api/v1/media/pronunciation-videos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "word": "Hausaufgabe",
						  "phrase": "Ich mache meine Hausaufgabe nach dem Abendessen.",
						  "language": "de"
						}
						"""))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.status").value("queued"));

		verify(pronunciationVideoService).create(any());
	}

	@Test
	void returnsCompletedPronunciationVideoStatus() throws Exception {
		UUID id = UUID.randomUUID();
		PronunciationVideoResponse response = new PronunciationVideoResponse(id, PronunciationVideoStatus.COMPLETED)
				.videoUrl(URI.create("https://media.fake.local/pronunciation-videos/%s/video.txt".formatted(id)))
				.expiresAt(OffsetDateTime.parse("2026-05-15T12:00:00Z"));
		when(pronunciationVideoService.get(id)).thenReturn(response);

		mockMvc.perform(get("/api/v1/media/pronunciation-videos/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.status").value("completed"))
				.andExpect(jsonPath("$.videoUrl").exists())
				.andExpect(jsonPath("$.expiresAt").exists());
	}

	@Test
	void rejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/api/v1/media/pronunciation-videos")
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
		when(pronunciationVideoService.get(id))
				.thenThrow(new PronunciationVideoNotFoundException("Pronunciation video asset was not found"));

		mockMvc.perform(get("/api/v1/media/pronunciation-videos/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("not_found"));
	}

}

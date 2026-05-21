package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PronunciationVideoRequest;
import com.vocavista.backend.api.model.PronunciationVideoResponse;
import com.vocavista.backend.api.model.PronunciationVideoStatus;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class PronunciationVideoCommandRunnerTest {

	@Test
	void waitsForCompletedGeneration() throws Exception {
		UUID id = UUID.randomUUID();
		PronunciationVideoService service = mock(PronunciationVideoService.class);
		when(service.create(new PronunciationVideoRequest("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				PronunciationVideoRequest.LanguageEnum.DE)))
				.thenReturn(new PronunciationVideoResponse(id, PronunciationVideoStatus.QUEUED));
		when(service.get(id)).thenReturn(new PronunciationVideoResponse(id, PronunciationVideoStatus.COMPLETED));
		PronunciationVideoCommandRunner runner = runner(service);

		PronunciationVideoResponse response = runner.runCommand();

		assertThat(response.getStatus()).isEqualTo(PronunciationVideoStatus.COMPLETED);
		verify(service).get(id);
	}

	private static PronunciationVideoCommandRunner runner(PronunciationVideoService service) {
		PronunciationVideoCommandProperties properties = new PronunciationVideoCommandProperties();
		properties.setWord("Hausaufgabe");
		properties.setPhrase("Ich mache meine Hausaufgabe.");
		properties.setLanguage("de");
		properties.setTimeoutSeconds(1);
		properties.setPollIntervalMillis(1);
		return new PronunciationVideoCommandRunner(service, properties, mock(ConfigurableApplicationContext.class),
				Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC), mock(PrintStream.class));
	}

}

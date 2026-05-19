package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DidLipSyncVideoProviderTest {

	@Test
	void createsTalkFromAudioUrlAndDownloadsCompletedVideo() {
		RestClient.Builder didBuilder = RestClient.builder().baseUrl("https://api.d-id.test");
		MockRestServiceServer didServer = MockRestServiceServer.bindTo(didBuilder).build();
		RestClient.Builder downloadBuilder = RestClient.builder();
		MockRestServiceServer downloadServer = MockRestServiceServer.bindTo(downloadBuilder).build();
		DidLipSyncVideoProvider provider = new DidLipSyncVideoProvider(didBuilder.build(), downloadBuilder.build(),
				"did-key", "https://images.test/avatar.png", 120, 1,
				Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC));

		didServer.expect(requestTo("https://api.d-id.test/talks"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Basic did-key"))
				.andExpect(content().json("""
						{
						  "source_url": "https://images.test/avatar.png",
						  "script": {
						    "type": "audio",
						    "audio_url": "https://media.test/audio.mp3"
						  },
						  "name": "vocavista-pronunciation-Hausaufgabe"
						}
						"""))
				.andRespond(withSuccess("{\"id\":\"tlk_123\",\"status\":\"created\"}", MediaType.APPLICATION_JSON));
		didServer.expect(requestTo("https://api.d-id.test/talks/tlk_123"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Basic did-key"))
				.andRespond(withSuccess(
						"{\"id\":\"tlk_123\",\"status\":\"done\",\"result_url\":\"https://result.test/video.mp4\"}",
						MediaType.APPLICATION_JSON));
		downloadServer.expect(requestTo("https://result.test/video.mp4"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("mp4-bytes".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

		GeneratedVideo video = provider.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe.\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german", "default-talking-head"), new GeneratedAudio("mp3".getBytes(), "audio/mpeg"),
				URI.create("https://media.test/audio.mp3"));

		assertThat(video.contentType()).isEqualTo("video/mp4");
		assertThat(video.bytes()).isEqualTo("mp4-bytes".getBytes());
		didServer.verify();
		downloadServer.verify();
	}

}

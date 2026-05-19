package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ElevenLabsTextToSpeechProviderTest {

	@Test
	void createsSpeechWithConfiguredVoiceAndModel() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.elevenlabs.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ElevenLabsTextToSpeechProvider provider = new ElevenLabsTextToSpeechProvider(builder.build(), "api-key",
				"voice-id", "eleven_multilingual_v2", "mp3_44100_128", 0.5, 0.75, 0.0, 0.72, 1.0, 0.86,
				true);

		server.expect(requestTo(
				"https://api.elevenlabs.test/v1/text-to-speech/voice-id?output_format=mp3_44100_128"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("xi-api-key", "api-key"))
				.andExpect(content().json("""
						{
						  "text": "Hausaufgabe...",
						  "model_id": "eleven_multilingual_v2",
						  "language_code": "de",
						  "voice_settings": {
						    "stability": 0.5,
						    "similarity_boost": 0.75,
						    "style": 0.0,
						    "speed": 0.72,
						    "use_speaker_boost": true
						  }
						}
						"""))
				.andRespond(withSuccess("first-".getBytes(), MediaType.APPLICATION_OCTET_STREAM));
		server.expect(requestTo(
				"https://api.elevenlabs.test/v1/text-to-speech/voice-id?output_format=mp3_44100_128"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("xi-api-key", "api-key"))
				.andExpect(content().json("""
						{
						  "text": "Hausaufgabe!",
						  "model_id": "eleven_multilingual_v2",
						  "language_code": "de",
						  "voice_settings": {
						    "stability": 0.5,
						    "similarity_boost": 0.75,
						    "style": 0.0,
						    "speed": 1.0,
						    "use_speaker_boost": true
						  }
						}
						"""))
				.andRespond(withSuccess("second-".getBytes(), MediaType.APPLICATION_OCTET_STREAM));
		server.expect(requestTo(
				"https://api.elevenlabs.test/v1/text-to-speech/voice-id?output_format=mp3_44100_128"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("xi-api-key", "api-key"))
				.andExpect(content().json("""
						{
						  "text": "Ich mache meine Hausaufgabe.",
						  "model_id": "eleven_multilingual_v2",
						  "language_code": "de",
						  "voice_settings": {
						    "stability": 0.5,
						    "similarity_boost": 0.75,
						    "style": 0.0,
						    "speed": 0.86,
						    "use_speaker_boost": true
						  }
						}
						"""))
				.andRespond(withSuccess("phrase".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

		GeneratedAudio audio = provider.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe.\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german", "default-talking-head"));

		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.bytes()).isEqualTo("first-second-phrase".getBytes());
		server.verify();
	}

}

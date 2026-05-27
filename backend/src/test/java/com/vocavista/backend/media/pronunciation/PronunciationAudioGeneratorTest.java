package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.retry.TransientAiException;

class PronunciationAudioGeneratorTest {

	@Test
	void createsSpeechWithConfiguredVoiceModelAndFormat() {
		TextToSpeechModel speechModel = mock(TextToSpeechModel.class);
		when(speechModel.call(any(TextToSpeechPrompt.class))).thenReturn(response("audio".getBytes()));
		when(speechModel.getDefaultOptions()).thenReturn(speechOptions());
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(speechModel);

		GeneratedAudio audio = generator.generate(new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german", "adult German speaker"));

		ArgumentCaptor<TextToSpeechPrompt> prompt = ArgumentCaptor.forClass(TextToSpeechPrompt.class);
		verify(speechModel).call(prompt.capture());
		assertThat(prompt.getValue().getInstructions().getText())
				.isEqualTo("Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.");
		assertThat(prompt.getValue().getOptions()).isNotInstanceOf(OpenAiAudioSpeechOptions.class);
		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.bytes()).isEqualTo("audio".getBytes());
		assertThat(generator.providerName()).isEqualTo("openai");
		assertThat(generator.modelName()).contains("gpt-4o-mini-tts", "coral", "mp3");
	}

	@Test
	void mapsProviderErrors() {
		TextToSpeechModel speechModel = mock(TextToSpeechModel.class);
		when(speechModel.call(any(TextToSpeechPrompt.class))).thenThrow(new TransientAiException("unavailable"));
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(speechModel);

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI speech generation failed");
	}

	@Test
	void rejectsEmptyAudio() {
		TextToSpeechModel speechModel = mock(TextToSpeechModel.class);
		when(speechModel.call(any(TextToSpeechPrompt.class))).thenReturn(response(new byte[0]));
		PronunciationAudioGenerator generator = new PronunciationAudioGenerator(speechModel);

		assertThatThrownBy(() -> generator.generate(script()))
				.isInstanceOf(MediaGenerationException.class)
				.hasMessage("OpenAI returned empty audio");
	}

	private static TextToSpeechResponse response(byte[] audio) {
		return new TextToSpeechResponse(List.of(new Speech(audio)));
	}

	private static OpenAiAudioSpeechOptions speechOptions() {
		return OpenAiAudioSpeechOptions.builder()
				.model(OpenAiAudioSpeechOptions.DEFAULT_SPEECH_MODEL)
				.voice(OpenAiAudioSpeechOptions.Voice.CORAL)
				.responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3)
				.build();
	}

	private static PronunciationScript script() {
		return new PronunciationScript("Hausaufgabe", "Ich mache meine Hausaufgabe.", "de",
				"Hausaufgabe...\n\nHausaufgabe!\n\nIch mache meine Hausaufgabe.", "v2",
				"default-clear-german", "adult German speaker");
	}

}

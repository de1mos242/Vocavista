package com.vocavista.backend.media.pronunciation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("vocavista.media.openai")
public class OpenAiTextToSpeechProperties {

	private String apiKey = "__missing__";
	private String model = "gpt-4o-mini-tts";
	private String voice = "coral";
	private String responseFormat = "mp3";
	private String instructions = "Speak in lively, warm, expressive German like an encouraging language tutor. Keep pronunciation precise and natural, with real intonation rather than a flat reading. Say the target word slowly and clearly, pause, say it again with friendly confidence, pause, then say the full phrase naturally with conversational energy.";

}

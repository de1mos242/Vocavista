package com.vocavista.backend.media.pronunciation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("vocavista.media.openai")
public class OpenAiTextToSpeechProperties {

	private String baseUrl = "https://api.openai.com";
	private String apiKey = "__missing__";
	private String model = "gpt-4o-mini-tts";
	private String voice = "coral";
	private String responseFormat = "mp3";
	private String instructions = "Speak in lively, warm, expressive German like an encouraging language tutor. Keep pronunciation precise and natural, with real intonation rather than a flat reading. Say the target word slowly and clearly, pause, say it again with friendly confidence, pause, then say the full phrase naturally with conversational energy.";

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

}

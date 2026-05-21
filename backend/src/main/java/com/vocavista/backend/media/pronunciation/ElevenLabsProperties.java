package com.vocavista.backend.media.pronunciation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("vocavista.media.elevenlabs")
public class ElevenLabsProperties {

	private String baseUrl = "https://api.elevenlabs.io";
	private String apiKey = "__missing__";
	private String voiceId = "FGY2WhTYpPnrIDTdsKH5";
	private String modelId = "eleven_multilingual_v2";
	private String outputFormat = "mp3_44100_128";
	private double stability = 0.5;
	private double similarityBoost = 0.75;
	private double style = 0.0;
	private double firstWordSpeed = 0.72;
	private double secondWordSpeed = 0.9;
	private double phraseSpeed = 0.86;
	private boolean useSpeakerBoost = true;

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

	public String getVoiceId() {
		return voiceId;
	}

	public void setVoiceId(String voiceId) {
		this.voiceId = voiceId;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public double getStability() {
		return stability;
	}

	public void setStability(double stability) {
		this.stability = stability;
	}

	public double getSimilarityBoost() {
		return similarityBoost;
	}

	public void setSimilarityBoost(double similarityBoost) {
		this.similarityBoost = similarityBoost;
	}

	public double getStyle() {
		return style;
	}

	public void setStyle(double style) {
		this.style = style;
	}

	public double getFirstWordSpeed() {
		return firstWordSpeed;
	}

	public void setFirstWordSpeed(double firstWordSpeed) {
		this.firstWordSpeed = firstWordSpeed;
	}

	public double getSecondWordSpeed() {
		return secondWordSpeed;
	}

	public void setSecondWordSpeed(double secondWordSpeed) {
		this.secondWordSpeed = secondWordSpeed;
	}

	public double getPhraseSpeed() {
		return phraseSpeed;
	}

	public void setPhraseSpeed(double phraseSpeed) {
		this.phraseSpeed = phraseSpeed;
	}

	public boolean isUseSpeakerBoost() {
		return useSpeakerBoost;
	}

	public void setUseSpeakerBoost(boolean useSpeakerBoost) {
		this.useSpeakerBoost = useSpeakerBoost;
	}

}

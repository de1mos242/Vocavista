package com.vocavista.backend.media.pronunciation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vocavista.media.veo")
public class VeoProperties {

	private String apiKey = "__missing__";
	private String baseUrl = "https://generativelanguage.googleapis.com";
	private String model = "veo-3.1-lite-generate-preview";
	private String aspectRatio = "9:16";
	private String personGeneration;
	private int sampleCount = 1;
	private int durationSeconds = 6;
	private Duration pollInterval = Duration.ofSeconds(5);
	private Duration timeout = Duration.ofMinutes(8);

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getAspectRatio() {
		return aspectRatio;
	}

	public void setAspectRatio(String aspectRatio) {
		this.aspectRatio = aspectRatio;
	}

	public String getPersonGeneration() {
		return personGeneration;
	}

	public void setPersonGeneration(String personGeneration) {
		this.personGeneration = personGeneration;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	public int getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(int durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public Duration getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(Duration pollInterval) {
		this.pollInterval = pollInterval;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

}

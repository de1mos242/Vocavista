package com.vocavista.backend.media.pronunciation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vocavista.media.pronunciation-video-command")
public class PronunciationVideoCommandProperties {

	private boolean enabled;
	private String word;
	private String phrase;
	private String language = "de";
	private long timeoutSeconds = 120;
	private long pollIntervalMillis = 500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getPhrase() {
		return phrase;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public long getPollIntervalMillis() {
		return pollIntervalMillis;
	}

	public void setPollIntervalMillis(long pollIntervalMillis) {
		this.pollIntervalMillis = pollIntervalMillis;
	}

}

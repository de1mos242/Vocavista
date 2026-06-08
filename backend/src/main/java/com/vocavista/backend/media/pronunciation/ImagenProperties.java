package com.vocavista.backend.media.pronunciation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vocavista.media.imagen")
public class ImagenProperties {

	private String apiKey = "__missing__";
	private String baseUrl = "https://generativelanguage.googleapis.com";
	private String model = "imagen-4.0-generate-001";
	private String aspectRatio = "16:9";
	private String sampleImageSize = "1K";
	private int sampleCount = 1;

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

	public String getSampleImageSize() {
		return sampleImageSize;
	}

	public void setSampleImageSize(String sampleImageSize) {
		this.sampleImageSize = sampleImageSize;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

}

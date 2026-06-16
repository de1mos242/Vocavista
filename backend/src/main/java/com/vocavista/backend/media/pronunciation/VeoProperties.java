package com.vocavista.backend.media.pronunciation;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vocavista.media.veo")
@Getter
@Setter
public class VeoProperties {

	private String apiKey = "__missing__";
	private String baseUrl = "https://generativelanguage.googleapis.com";
	private String model = "veo-3.1-lite-generate-preview";
	private String aspectRatio = "9:16";
	private String resolution = "720p";
	private String personGeneration;
	private int sampleCount = 1;
	private int durationSeconds = 6;
	private Duration pollInterval = Duration.ofSeconds(5);
	private Duration timeout = Duration.ofMinutes(8);

}

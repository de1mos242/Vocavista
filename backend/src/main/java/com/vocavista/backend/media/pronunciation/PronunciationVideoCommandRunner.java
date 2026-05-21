package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PronunciationVideoRequest;
import com.vocavista.backend.api.model.PronunciationVideoResponse;
import com.vocavista.backend.api.model.PronunciationVideoStatus;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media.pronunciation-video-command", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PronunciationVideoCommandProperties.class)
class PronunciationVideoCommandRunner implements CommandLineRunner {

	private final PronunciationVideoService pronunciationVideoService;
	private final PronunciationVideoCommandProperties properties;
	private final ConfigurableApplicationContext applicationContext;
	private final Clock clock;
	private final PrintStream out;

	@Autowired
	PronunciationVideoCommandRunner(
			PronunciationVideoService pronunciationVideoService,
			PronunciationVideoCommandProperties properties,
			ConfigurableApplicationContext applicationContext) {
		this(pronunciationVideoService, properties, applicationContext, Clock.systemUTC(), System.out);
	}

	PronunciationVideoCommandRunner(
			PronunciationVideoService pronunciationVideoService,
			PronunciationVideoCommandProperties properties,
			ConfigurableApplicationContext applicationContext,
			Clock clock,
			PrintStream out) {
		this.pronunciationVideoService = pronunciationVideoService;
		this.properties = properties;
		this.applicationContext = applicationContext;
		this.clock = clock;
		this.out = out;
	}

	@Override
	public void run(String... args) throws Exception {
		try {
			PronunciationVideoResponse response = runCommand();
			print(response);
		}
		finally {
			applicationContext.close();
		}
	}

	PronunciationVideoResponse runCommand() throws InterruptedException {
		PronunciationVideoResponse response = pronunciationVideoService.create(request());
		Instant deadline = clock.instant().plus(Duration.ofSeconds(properties.getTimeoutSeconds()));
		while (!isTerminal(response.getStatus()) && clock.instant().isBefore(deadline)) {
			Thread.sleep(properties.getPollIntervalMillis());
			response = pronunciationVideoService.get(response.getId());
		}
		if (!isTerminal(response.getStatus())) {
			throw new PronunciationVideoCommandTimeoutException("Pronunciation media generation did not finish before timeout");
		}
		return response;
	}

	private PronunciationVideoRequest request() {
		PronunciationVideoRequest.LanguageEnum language = PronunciationVideoRequest.LanguageEnum.fromValue(properties.getLanguage());
		return new PronunciationVideoRequest(properties.getWord(), properties.getPhrase(), language);
	}

	private void print(PronunciationVideoResponse response) {
		out.println("id=" + response.getId());
		out.println("status=" + response.getStatus());
		if (response.getAudioUrl() != null) {
			out.println("audioUrl=" + response.getAudioUrl());
		}
		if (response.getRenderMode() != null) {
			out.println("renderMode=" + response.getRenderMode());
		}
		if (response.getErrorCode() != null) {
			out.println("errorCode=" + response.getErrorCode());
		}
		if (response.getErrorMessage() != null) {
			out.println("errorMessage=" + response.getErrorMessage());
		}
	}

	private static boolean isTerminal(PronunciationVideoStatus status) {
		return status == PronunciationVideoStatus.COMPLETED || status == PronunciationVideoStatus.FAILED;
	}

}

class PronunciationVideoCommandTimeoutException extends RuntimeException {

	PronunciationVideoCommandTimeoutException(String message) {
		super(message);
	}

}

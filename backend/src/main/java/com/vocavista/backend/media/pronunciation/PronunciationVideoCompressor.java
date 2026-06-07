package com.vocavista.backend.media.pronunciation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class PronunciationVideoCompressor {

	private final String ffmpegPath;
	private final Duration timeout;

	PronunciationVideoCompressor(
			@Value("${vocavista.media.video-compression.ffmpeg-path:ffmpeg}") String ffmpegPath,
			@Value("${vocavista.media.video-compression.timeout:60s}") Duration timeout) {
		this.ffmpegPath = ffmpegPath;
		this.timeout = timeout;
	}

	Optional<GeneratedVideo> compress(GeneratedVideo video) {
		Path input = null;
		Path output = null;
		try {
			input = Files.createTempFile("vocavista-pronunciation-", ".mp4");
			output = Files.createTempFile("vocavista-pronunciation-small-", ".mp4");
			Files.write(input, video.bytes());

			List<String> command = List.of(
					ffmpegPath,
					"-y",
					"-i", input.toString(),
					"-c:v", "libx264",
					"-preset", "slow",
					"-crf", "30",
					"-maxrate", "650k",
					"-bufsize", "1300k",
					"-pix_fmt", "yuv420p",
					"-movflags", "+faststart",
					"-c:a", "aac",
					"-b:a", "64k",
					output.toString());
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!completed) {
				process.destroyForcibly();
				log.warn("Pronunciation video compression timed out after {}", timeout);
				return Optional.empty();
			}
			if (process.exitValue() != 0) {
				log.warn("Pronunciation video compression failed with exit code {}", process.exitValue());
				return Optional.empty();
			}

			byte[] compressedBytes = Files.readAllBytes(output);
			if (compressedBytes.length == 0 || compressedBytes.length >= video.bytes().length) {
				log.info("Skipping compressed pronunciation video because it is not smaller than the original");
				return Optional.empty();
			}
			return Optional.of(new GeneratedVideo(compressedBytes, "video/mp4"));
		}
		catch (IOException ex) {
			log.warn("Pronunciation video compression is unavailable", ex);
			return Optional.empty();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("Pronunciation video compression was interrupted", ex);
			return Optional.empty();
		}
		finally {
			deleteIfExists(input);
			deleteIfExists(output);
		}
	}

	private static void deleteIfExists(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ex) {
			log.warn("Could not delete temporary pronunciation video file {}", path, ex);
		}
	}

}

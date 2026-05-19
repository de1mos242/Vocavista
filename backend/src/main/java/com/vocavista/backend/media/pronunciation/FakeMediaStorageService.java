package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "storage-mode", havingValue = "fake", matchIfMissing = true)
class FakeMediaStorageService implements MediaStorageService {

	private final Map<String, byte[]> storedObjects = new ConcurrentHashMap<>();
	private final Clock clock;

	FakeMediaStorageService() {
		this(Clock.systemUTC());
	}

	FakeMediaStorageService(Clock clock) {
		this.clock = clock;
	}

	@Override
	public void store(String objectKey, String contentType, byte[] bytes) {
		storedObjects.put(objectKey, bytes.clone());
	}

	@Override
	public PlayableMedia playableUrl(String objectKey) {
		return new PlayableMedia(URI.create("https://media.fake.local/" + objectKey),
				OffsetDateTime.now(clock).plusHours(1));
	}

}

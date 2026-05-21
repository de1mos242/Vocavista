package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "storage-mode", havingValue = "fake", matchIfMissing = true)
class FakeMediaStorageService implements MediaStorageService {

	private final ConcurrentHashMap<String, StoredMedia> storedObjects = new ConcurrentHashMap<>();
	private final Clock clock;

	FakeMediaStorageService() {
		this(Clock.systemUTC());
	}

	FakeMediaStorageService(Clock clock) {
		this.clock = clock;
	}

	@Override
	public void store(String objectKey, String contentType, byte[] bytes) {
		storedObjects.put(objectKey, new StoredMedia(contentType, bytes.clone()));
	}

	@Override
	public StoredMedia read(String objectKey) {
		StoredMedia storedMedia = storedObjects.get(objectKey);
		if (storedMedia == null) {
			throw new PronunciationVideoNotFoundException("Generated media object was not found");
		}
		return new StoredMedia(storedMedia.contentType(), storedMedia.bytes().clone());
	}

	@Override
	public PlayableMedia playableUrl(String objectKey) {
		return new PlayableMedia(URI.create("https://media.fake.local/" + objectKey),
				OffsetDateTime.now(clock).plusHours(1));
	}

}

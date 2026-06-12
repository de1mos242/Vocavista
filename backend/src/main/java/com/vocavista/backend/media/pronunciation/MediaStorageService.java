package com.vocavista.backend.media.pronunciation;

interface MediaStorageService {

	void store(String objectKey, String contentType, byte[] bytes);

	StoredMedia read(String objectKey);

}

record StoredMedia(String contentType, byte[] bytes) {
}

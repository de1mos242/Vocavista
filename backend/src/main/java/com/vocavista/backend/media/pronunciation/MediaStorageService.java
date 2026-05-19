package com.vocavista.backend.media.pronunciation;

interface MediaStorageService {

	void store(String objectKey, String contentType, byte[] bytes);

	PlayableMedia playableUrl(String objectKey);

}

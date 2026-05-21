package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(prefix = "vocavista.media", name = "storage-mode", havingValue = "s3")
class S3MediaStorageService implements MediaStorageService {

	private final S3Client s3Client;
	private final String bucket;

	@Autowired
	S3MediaStorageService(
			@Value("${vocavista.media.s3.endpoint}") String endpoint,
			@Value("${vocavista.media.s3.region}") String region,
			@Value("${vocavista.media.s3.bucket}") String bucket,
			@Value("${vocavista.media.s3.access-key}") String accessKey,
			@Value("${vocavista.media.s3.secret-key}") String secretKey,
			@Value("${vocavista.media.s3.path-style-access:true}") boolean pathStyleAccess) {
		this(bucket, buildClient(endpoint, region, accessKey, secretKey, pathStyleAccess));
	}

	S3MediaStorageService(String bucket, S3Client s3Client) {
		this.bucket = bucket;
		this.s3Client = s3Client;
	}

	@Override
	public void store(String objectKey, String contentType, byte[] bytes) {
		try {
			s3Client.putObject(PutObjectRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.contentType(contentType)
					.build(), RequestBody.fromBytes(bytes));
		}
		catch (RuntimeException ex) {
			throw new MediaGenerationException("storage_error", "Could not store generated media", ex);
		}
	}

	@Override
	public StoredMedia read(String objectKey) {
		try {
			var responseBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.build());
			return new StoredMedia(responseBytes.response().contentType(), responseBytes.asByteArray());
		}
		catch (RuntimeException ex) {
			throw new PronunciationVideoNotFoundException("Generated media object was not found", ex);
		}
	}

	private static S3Client buildClient(
			String endpoint,
			String region,
			String accessKey,
			String secretKey,
			boolean pathStyleAccess) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build());
		if (StringUtils.hasText(endpoint)) {
			builder.endpointOverride(URI.create(endpoint));
		}
		return builder.build();
	}

}

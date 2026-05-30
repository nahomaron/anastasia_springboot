package com.anastasia.Anastasia_BackEnd.common.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Profile({"dev", "staging", "prod"})
@RequiredArgsConstructor
public class AwsS3Service implements S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.prefix:}")
    private String bucketPrefix;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String objectKey, String contentType) {
        String storageKey = prefixedKey(objectKey);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(null, objectKey, buildObjectUrl(objectKey), presignedRequest.url().toString(), contentType);
    }

    @Override
    public StoredObjectMetadata verifyUploadedObject(String objectKey) {
        HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(prefixedKey(objectKey))
                .build());
        return new StoredObjectMetadata(
                objectKey,
                buildObjectUrl(objectKey),
                response.contentType(),
                response.contentLength()
        );
    }

    @Override
    public String buildObjectUrl(String objectKey) {
        String storageKey = prefixedKey(objectKey);
        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            String base = s3Endpoint.endsWith("/") ? s3Endpoint.substring(0, s3Endpoint.length() - 1) : s3Endpoint;
            return base + "/" + bucketName + "/" + storageKey;
        }
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + storageKey;
    }

    private String prefixedKey(String objectKey) {
        if (bucketPrefix == null || bucketPrefix.isBlank()) {
            return objectKey;
        }
        String normalizedPrefix = bucketPrefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        return normalizedPrefix + "/" + objectKey;
    }
}

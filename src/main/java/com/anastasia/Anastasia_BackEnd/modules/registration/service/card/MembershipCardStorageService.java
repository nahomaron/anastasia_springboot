package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MembershipCardStorageService {

    private static final Map<String, byte[]> IN_MEMORY_OBJECTS = new ConcurrentHashMap<>();

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Value("${aws.s3.endpoint:http://localhost:4566}")
    private String s3Endpoint;

    private final S3Client s3Client;

    public void upload(String objectKey, byte[] content, String contentType) {
        requireBucketName();
        if (isFallbackEnabled()) {
            IN_MEMORY_OBJECTS.put(objectKey, content.clone());
            return;
        }
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
    }

    public byte[] read(String objectKey) {
        requireBucketName();
        if (isFallbackEnabled()) {
            byte[] content = IN_MEMORY_OBJECTS.get(objectKey);
            if (content == null) {
                throw new IllegalStateException("Membership card object not found: " + objectKey);
            }
            return content.clone();
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    private void requireBucketName() {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("AWS S3 bucket is not configured for membership cards");
        }
    }

    private boolean isFallbackEnabled() {
        return s3Endpoint != null && (s3Endpoint.contains("localhost")
                || s3Endpoint.contains("127.0.0.1")
                || s3Endpoint.contains("0.0.0.0"));
    }
}

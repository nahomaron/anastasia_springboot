package com.anastasia.Anastasia_BackEnd.common.aws;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Primary
@Profile({"test", "api-tests"})
public class TestS3Service implements S3Service {

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String objectKey, String contentType) {
        String resolvedObjectKey = objectKey == null || objectKey.isBlank()
                ? "test-imageAssets/" + UUID.randomUUID()
                : objectKey;
        return new PresignedUrlResponse(null, resolvedObjectKey, buildObjectUrl(resolvedObjectKey), "http://localhost/mock-presigned-url", contentType);
    }

    @Override
    public StoredObjectMetadata verifyUploadedObject(String objectKey) {
        String resolvedObjectKey = objectKey == null || objectKey.isBlank()
                ? "test-imageAssets/" + UUID.randomUUID()
                : objectKey;
        return new StoredObjectMetadata(resolvedObjectKey, buildObjectUrl(resolvedObjectKey), "image/png", 1024L);
    }

    @Override
    public String buildObjectUrl(String objectKey) {
        return "http://localhost/mock-bucket/" + objectKey;
    }
}

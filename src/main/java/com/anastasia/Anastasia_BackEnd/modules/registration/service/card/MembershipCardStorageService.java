package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class MembershipCardStorageService {

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    private final S3Client s3Client;

    public void upload(String objectKey, byte[] content, String contentType) {
        requireBucketName();
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
}

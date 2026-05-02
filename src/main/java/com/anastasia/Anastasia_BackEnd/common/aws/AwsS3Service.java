package com.anastasia.Anastasia_BackEnd.common.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@Profile({"dev", "staging", "prod"})
@RequiredArgsConstructor
public class AwsS3Service implements S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.prefix:}")
    private String bucketPrefix;

    private final S3Presigner presigner;

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String fileName) {
        String objectKey = prefixedKey("images/" + UUID.randomUUID() + "_" + fileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("image/jpeg")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(objectKey, presignedRequest.url().toString());
    }

    private String prefixedKey(String objectKey) {
        if (bucketPrefix == null || bucketPrefix.isBlank()) {
            return objectKey;
        }
        String normalizedPrefix = bucketPrefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        return normalizedPrefix + "/" + objectKey;
    }
}

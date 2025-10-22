package com.anastasia.Anastasia_BackEnd.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class S3TemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(S3TemplateLoader.class);

    private final S3Client s3Client;
    private final String bucketName;

    public S3TemplateLoader(S3Client s3Client,
                            @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String loadTemplate(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response =
                         s3Client.getObject(request)) {
                return new String(response.readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (SdkClientException | IOException e) {
            log.warn("Template not found in S3: {}", key);
            return null;
        }
    }
}

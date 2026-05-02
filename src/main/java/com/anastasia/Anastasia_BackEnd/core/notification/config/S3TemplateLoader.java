package com.anastasia.Anastasia_BackEnd.core.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class S3TemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(S3TemplateLoader.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String bucketPrefix;
    private final boolean enabled;

    public S3TemplateLoader(S3Client s3Client,
                            @Value("${aws.s3.bucket:}") String bucketName,
                            @Value("${aws.s3.prefix:}") String bucketPrefix,
                            @Value("${aws.s3.templates.enabled:true}") boolean enabled) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.bucketPrefix = bucketPrefix;
        this.enabled = enabled;
    }

    public String loadTemplate(String key) {
        if (!enabled) {
            log.debug("S3 template loading is disabled. Skipping key {}", key);
            return null;
        }
        if (!StringUtils.hasText(bucketName)) {
            log.debug("S3 bucket name not configured. Skipping key {}", key);
            return null;
        }

        try {
            String resolvedKey = prefixedKey(key);
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(resolvedKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response =
                         s3Client.getObject(request)) {
                return new String(response.readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (NoSuchKeyException e) {
            log.warn("Template key {} was not found in S3 bucket {}", key, bucketName);
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn("Template key {} is missing in S3 bucket {}", key, bucketName);
                return null;
            }
            throw e;
        } catch (SdkClientException | IOException e) {
            log.warn("Template could not be loaded from S3 key {}: {}", key, e.getMessage());
            return null;
        }
    }

    private String prefixedKey(String key) {
        if (!StringUtils.hasText(bucketPrefix)) {
            return key;
        }
        String normalizedPrefix = bucketPrefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        return normalizedPrefix + "/" + key;
    }
}

package com.anastasia.Anastasia_BackEnd.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Profile({"dev", "prod", "test", "api-tests"})
public class S3Config {

    @Value("${aws.accessKeyId:test}")
    private String awsAccessKey;

    @Value("${aws.secretKey:test}")
    private String awsSecretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.s3.endpoint:http://localhost:4566}")
    private String s3Endpoint;

    @Bean
    public S3Client s3Client() {
        boolean useEndpointOverride = StringUtils.hasText(s3Endpoint);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(useEndpointOverride)
                        .build());

        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider(useEndpointOverride);
        if (credentialsProvider != null) {
            builder = builder.credentialsProvider(credentialsProvider);
        }

        if (useEndpointOverride) {
            builder = builder.endpointOverride(URI.create(s3Endpoint));
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        boolean useEndpointOverride = StringUtils.hasText(s3Endpoint);
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(useEndpointOverride)
                        .build());

        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider(useEndpointOverride);
        if (credentialsProvider != null) {
            builder = builder.credentialsProvider(credentialsProvider);
        }

        if (useEndpointOverride) {
            builder = builder.endpointOverride(URI.create(s3Endpoint));
        }

        return builder.build();
    }

    private AwsCredentialsProvider resolveCredentialsProvider(boolean useEndpointOverride) {
        if (StringUtils.hasText(awsAccessKey) && StringUtils.hasText(awsSecretKey)) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
            return StaticCredentialsProvider.create(credentials);
        }

        if (useEndpointOverride) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
            return StaticCredentialsProvider.create(credentials);
        }

        return DefaultCredentialsProvider.create();
    }
}

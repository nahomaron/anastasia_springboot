package com.anastasia.Anastasia_BackEnd.common.aws;

/**
 * Abstraction for generating presigned upload URLs used by image upload workflows.
 * Concrete implementations can target real AWS infrastructure or provide
 * lightweight stubs for tests.
 */
public interface S3Service {
    PresignedUrlResponse generatePresignedUploadUrl(String fileName);
}

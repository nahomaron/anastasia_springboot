package com.anastasia.Anastasia_BackEnd.common.aws;

public record StoredObjectMetadata(
        String objectKey,
        String objectUrl,
        String contentType,
        long fileSizeBytes
) {
}

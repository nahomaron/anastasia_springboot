package com.anastasia.Anastasia_BackEnd.common.aws;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Primary
@Profile({"test", "test-server", "api"})
public class TestS3Service implements S3Service {

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String fileName) {
        String objectKey = "test-images/" + UUID.randomUUID() + "_" + fileName;
        return new PresignedUrlResponse(objectKey, "http://localhost/mock-presigned-url");
    }
}

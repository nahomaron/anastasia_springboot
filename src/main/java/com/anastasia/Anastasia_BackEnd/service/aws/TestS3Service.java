package com.anastasia.Anastasia_BackEnd.service.aws;

import com.anastasia.Anastasia_BackEnd.model.aws.PresignedUrlResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Primary
@Profile("test")
public class TestS3Service implements S3Service {

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String fileName) {
        String objectKey = "test-avatars/" + UUID.randomUUID() + "_" + fileName;
        return new PresignedUrlResponse(objectKey, "http://localhost/mock-presigned-url");
    }
}

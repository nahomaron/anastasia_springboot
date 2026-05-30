package com.anastasia.Anastasia_BackEnd.common.aws;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PresignedUrlResponse {
    private UUID uploadId;
    private String objectKey;
    private String objectUrl;
    private String uploadUrl;
    private String contentType;
}

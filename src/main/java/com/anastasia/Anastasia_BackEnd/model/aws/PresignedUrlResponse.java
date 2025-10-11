package com.anastasia.Anastasia_BackEnd.model.aws;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUrlResponse {
    private String objectKey;
    private String uploadUrl;
}


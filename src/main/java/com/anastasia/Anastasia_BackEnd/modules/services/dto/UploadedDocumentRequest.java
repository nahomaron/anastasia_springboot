package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadedDocumentRequest(
        @NotBlank(message = "imageUrl is required")
        @Size(max = 1024, message = "imageUrl is too long")
        String imageUrl,
        @Size(max = 64, message = "imageSize is too long")
        String imageSize
) {
}

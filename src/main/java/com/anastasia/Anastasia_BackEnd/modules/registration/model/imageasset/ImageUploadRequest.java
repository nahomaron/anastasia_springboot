package com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 255, message = "fileName is too long")
    private String fileName;

    @NotBlank(message = "contentType is required")
    @Size(max = 128, message = "contentType is too long")
    private String contentType;

    @NotNull(message = "fileSizeBytes is required")
    @Min(value = 1, message = "fileSizeBytes must be positive")
    @Max(value = 5_242_880, message = "fileSizeBytes exceeds the 5 MB limit")
    private Long fileSizeBytes;
}

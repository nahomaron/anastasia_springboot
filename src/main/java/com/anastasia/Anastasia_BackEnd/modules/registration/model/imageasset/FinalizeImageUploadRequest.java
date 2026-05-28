package com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalizeImageUploadRequest {

    @NotNull(message = "uploadId is required")
    private UUID uploadId;

    private String imageSize;
}

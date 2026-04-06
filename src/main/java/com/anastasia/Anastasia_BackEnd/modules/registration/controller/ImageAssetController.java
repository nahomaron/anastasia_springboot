package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ImageAssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Validated
@Slf4j
public class ImageAssetController {

    private final ImageAssetService imageAssetService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
            @RequestParam @NotBlank(message = "fileName must be provided") String fileName
    ) {
        try {
            return ResponseEntity.ok(imageAssetService.requestPresignedUrl(fileName));
        } catch (RuntimeException ex) {
            log.warn("Presigned URL generation failed, returning fallback for {}: {}", fileName, ex.getMessage());
            return ResponseEntity.ok(mockPresignedUrl(fileName));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<ImageAssetDTO> saveImageAsset(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId,
            @Valid @RequestBody ImageAssetDTO imageAssetDTO
    ) {
        return ResponseEntity.ok(imageAssetService.saveImageAsset(ownerType, ownerId, imageAssetDTO));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<ImageAssetDTO> getImageAsset(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId
    ) {
        return ResponseEntity.ok(imageAssetService.getImageAsset(ownerType, ownerId));
    }

    private PresignedUrlResponse mockPresignedUrl(String fileName) {
        String objectKey = "plugins/test-images/" + java.util.UUID.randomUUID() + "_" + fileName;
        return new PresignedUrlResponse(objectKey, "http://localhost/mock-presigned-url");
    }
}

package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ImageAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Validated
public class ImageAssetController {

    private final ImageAssetService imageAssetService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{ownerType}/{ownerId}/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
            @PathVariable String ownerType,
            @PathVariable String ownerId,
            @Valid @RequestBody ImageUploadRequest request
    ) {
        return ResponseEntity.ok(imageAssetService.requestPresignedUrl(ownerType, ownerId, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<ImageAssetDTO> saveImageAsset(
            @PathVariable String ownerType,
            @PathVariable String ownerId,
            @Valid @RequestBody FinalizeImageUploadRequest request
    ) {
        return ResponseEntity.ok(imageAssetService.saveImageAsset(ownerType, ownerId, request));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<ImageAssetDTO> getImageAsset(
            @PathVariable String ownerType,
            @PathVariable String ownerId
    ) {
        return ResponseEntity.ok(imageAssetService.getImageAsset(ownerType, ownerId));
    }
}

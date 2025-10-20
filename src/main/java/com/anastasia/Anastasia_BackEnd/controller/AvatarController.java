package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.model.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.service.avatar.AvatarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/avatars")
@Validated
public class AvatarController {

    private final AvatarService avatarService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
            @RequestParam @NotBlank(message = "fileName must be provided") String fileName
    ) {
        return ResponseEntity.ok(avatarService.requestPresignedUrl(fileName));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<AvatarDTO> saveAvatar(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId,
            @Valid @RequestBody AvatarDTO avatarDTO
    ) {
        return ResponseEntity.ok(avatarService.saveAvatar(ownerType, ownerId, avatarDTO));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<AvatarDTO> getAvatar(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId
    ) {
        return ResponseEntity.ok(avatarService.getAvatar(ownerType, ownerId));
    }

}

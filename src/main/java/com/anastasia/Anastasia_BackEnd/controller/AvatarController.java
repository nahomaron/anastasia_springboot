package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.model.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.service.avatar.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/avatars")
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(@RequestParam String fileName) {
        return ResponseEntity.ok(avatarService.requestPresignedUrl(fileName));
    }

    @PostMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<AvatarDTO> saveAvatar(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId,
            @RequestBody AvatarDTO avatarDTO
    ) {
        return ResponseEntity.ok(avatarService.saveAvatar(ownerType, ownerId, avatarDTO));
    }


    @GetMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<AvatarDTO> getAvatar(
            @PathVariable String ownerType,
            @PathVariable UUID ownerId
    ) {
        return ResponseEntity.ok(avatarService.getAvatar(ownerType, ownerId));
    }

}

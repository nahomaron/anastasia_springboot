package com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvatarDTO {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String imageSize; // Optional
}
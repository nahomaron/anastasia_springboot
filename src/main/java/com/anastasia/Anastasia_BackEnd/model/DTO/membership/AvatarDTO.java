package com.anastasia.Anastasia_BackEnd.model.DTO.membership;

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

    private Long id;
    private Long churchId;
    private UUID userId;
    private Long membershipId;
    private String imageUrl;
    private String imageSize;
}

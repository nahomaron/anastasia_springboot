package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;

public record MarriageReviewActionRequest(
        @NotBlank String reason,
        String notes
) {
}

package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;

public record MarriageScheduleCancellationRequest(@NotBlank String reason) {
}

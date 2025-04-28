package com.anastasia.Anastasia_BackEnd.model.user;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SimpleUserDTO(UUID uuid, String fullName, String email) {}

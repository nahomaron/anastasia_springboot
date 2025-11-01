package com.anastasia.Anastasia_BackEnd.modules.users.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SimpleUserDTO(UUID uuid, String fullName, String email) {}

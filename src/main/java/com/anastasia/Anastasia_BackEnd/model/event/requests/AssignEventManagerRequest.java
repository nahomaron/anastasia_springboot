package com.anastasia.Anastasia_BackEnd.model.event.requests;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AssignEventManagerRequest {
    private UUID userId;
    private String role;
}

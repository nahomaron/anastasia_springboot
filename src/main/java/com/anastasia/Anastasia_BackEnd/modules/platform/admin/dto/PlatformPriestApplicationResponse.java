package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PlatformPriestApplicationResponse {
    private String priestId;
    private String fullName;
    private List<String> languages;
    private PriestStatus status;
    private String location;
    private Instant submittedAt;
    private UUID assignedTenant;
    private int experienceYears;
    private String notes;
}

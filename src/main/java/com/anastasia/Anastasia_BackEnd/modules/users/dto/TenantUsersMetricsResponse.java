package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantUsersMetricsResponse {
    private long total;
    private long active;
    private long invited;
    private long disabled;
    private long locked;
}

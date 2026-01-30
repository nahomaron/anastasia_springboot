package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSubscriptionResponse {
    private UUID tenantId;
    private Long churchId;
}

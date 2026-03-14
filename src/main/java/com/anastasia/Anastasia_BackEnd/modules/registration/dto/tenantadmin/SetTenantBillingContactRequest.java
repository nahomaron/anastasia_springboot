package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetTenantBillingContactRequest {
    @NotNull
    private Boolean billingContact;
}

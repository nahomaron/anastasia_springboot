package com.anastasia.Anastasia_BackEnd.model.DTO;

import com.anastasia.Anastasia_BackEnd.model.entity.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantType;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {

    @NotNull(message = "Tenant type is required")
    private TenantType tenantType; // CHURCH or PRIEST

    @NotBlank(message = "Owner name is required")
    private String ownerName; // Can be a church name or a priest's full name

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email; // Contact email (Church or Priest)

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+|00)\\d{1,3}\\d{6,12}$", message = "Invalid international phone number format")
    private String phoneNumber; // Contact

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan subscriptionPlan; // Subscription Type
}

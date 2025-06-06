package com.anastasia.Anastasia_BackEnd.model.tenant;

import jakarta.validation.constraints.*;
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

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan subscriptionPlan; // Subscription Type

    @NotBlank(message = "Owner name is required")
    private String ownerName; // Can be a church name or a priest's full name

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email; // Contact email (Church or Priest)


    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+|00)\\d{1,3}\\d{6,12}$", message = "Invalid international phone number format")
    private String phoneNumber; // Contact

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain an uppercase letter, a lowercase letter, a number, and a special character")
    private String password;

    @NotBlank(message = "Confirm Password is required")
    private String confirmPassword;


    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }



}

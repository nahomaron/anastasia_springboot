package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {
    @NotNull(message = "{validation.tenant.type.required}")
    private TenantType tenantType; // CHURCH or PRIEST

    @NotNull(message = "{validation.tenant.subscriptionPlan.required}")
    private SubscriptionPlan subscriptionPlan; // Subscription Type

    @NotBlank(message = "{validation.tenant.ownerName.required}")
    private String ownerName; // Can be a church name or a priest's full name

    @NotBlank(message = "{validation.tenant.email.required}")
    @Email(message = "{validation.tenant.email.invalid}")
    private String email; // Contact email (Church or Priest)


    @NotBlank(message = "{validation.tenant.phone.required}")
    @Pattern(regexp = "^(?:\\+|00)\\d{1,3}\\d{6,12}$", message = "{validation.tenant.phone.invalid}")
    private String phoneNumber; // Contact

    @NotBlank(message = "{validation.tenant.password.required}")
    @Size(min = 8, message = "{validation.tenant.password.min}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "{validation.tenant.password.pattern}")
    private String password;

    @NotBlank(message = "{validation.tenant.confirmPassword.required}")
    private String confirmPassword;

    private Boolean termsAccepted;

    private String termsVersion;

    @Valid
    private ChurchDTO church;


    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }



}

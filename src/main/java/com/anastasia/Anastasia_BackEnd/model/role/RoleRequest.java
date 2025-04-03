package com.anastasia.Anastasia_BackEnd.model.role;

import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @NotEmpty(message = "Role name is required")
    private String roleName;

    private String description;

    private Set<PermissionType> permissions;

}

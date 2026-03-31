package com.anastasia.Anastasia_BackEnd.core.auth.role;

import java.util.Set;

public record AssignRolesRequest(Set<Long> roleIds) {

    public AssignRolesRequest(Set<Long> roleIds) {
        this.roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
    }
}

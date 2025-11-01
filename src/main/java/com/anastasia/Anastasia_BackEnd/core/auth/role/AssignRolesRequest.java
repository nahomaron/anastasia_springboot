package com.anastasia.Anastasia_BackEnd.core.auth.role;

import java.util.Set;

public record AssignRolesRequest(Set<Long> roleIds) {
}

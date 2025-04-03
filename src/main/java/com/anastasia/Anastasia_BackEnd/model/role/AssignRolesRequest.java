package com.anastasia.Anastasia_BackEnd.model.role;

import java.util.Set;

public record AssignRolesRequest(Set<Long> roleIds) {
}

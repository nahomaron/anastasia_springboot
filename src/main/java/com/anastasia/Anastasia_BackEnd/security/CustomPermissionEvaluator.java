package com.anastasia.Anastasia_BackEnd.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/*
This custom @Component named permissionEvaluator to enable fine-grained permission-based access control
using Spring Security's @PreAuthorize annotation.

Features
1 - Check multiple permissions dynamically
2 - Supports both hasAny and hasAll logic
3 - Cleaner alternative to chaining hasAuthority(...)
 */
@Component("permissionEvaluator")
public class CustomPermissionEvaluator {

    public boolean hasAny(Authentication authentication, String... permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(permissions)
                .anyMatch(userAuthorities::contains);
    }

    public boolean hasAll(Authentication authentication, String... permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(permissions)
                .allMatch(userAuthorities::contains);
    }
}

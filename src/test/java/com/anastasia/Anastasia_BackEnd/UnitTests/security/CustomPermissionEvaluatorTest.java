package com.anastasia.Anastasia_BackEnd.UnitTests.security;

import com.anastasia.Anastasia_BackEnd.common.security.CustomPermissionEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomPermissionEvaluatorTest {

    private final CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator();

    private Authentication authenticationWith(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void hasAny_shouldReturnTrueWhenAnyPermissionMatches() {
        Authentication auth = authenticationWith("MANAGE_GROUPS", "VIEW_USERS");

        assertThat(evaluator.hasAny(auth, "EDIT_USERS", "MANAGE_GROUPS")).isTrue();
    }

    @Test
    void hasAny_shouldReturnFalseWhenNoMatch() {
        Authentication auth = authenticationWith("VIEW_USERS");

        assertThat(evaluator.hasAny(auth, "EDIT_USERS", "DELETE_USERS")).isFalse();
    }

    @Test
    void hasAll_shouldReturnTrueWhenAllPermissionsPresent() {
        Authentication auth = authenticationWith("MANAGE_GROUPS", "VIEW_GROUPS", "DELETE_GROUPS");

        assertThat(evaluator.hasAll(auth, "MANAGE_GROUPS", "VIEW_GROUPS")).isTrue();
    }

    @Test
    void hasAll_shouldReturnFalseWhenAnyPermissionMissing() {
        Authentication auth = authenticationWith("VIEW_GROUPS");

        assertThat(evaluator.hasAll(auth, "MANAGE_GROUPS", "VIEW_GROUPS")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAuthenticationInvalid() {
        assertThat(evaluator.hasAny(null, "PERM")).isFalse();
        Authentication anonymous = authenticationWith();
        ((UsernamePasswordAuthenticationToken) anonymous).setAuthenticated(false);
        assertThat(evaluator.hasAll(anonymous, "PERM")).isFalse();
    }
}

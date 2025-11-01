package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasPermission_shouldReturnTrueWhenAuthorityExists() {
        var auth = new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority("MANAGE_GROUPS"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(securityUtils.hasPermission("MANAGE_GROUPS")).isTrue();
        assertThat(securityUtils.hasPermission("VIEW_USERS")).isFalse();
    }

    @Test
    void generateUniqueIDNumber_shouldIncludeBaseLetterAndLength() {
        String code = securityUtils.generateUniqueIDNumber(6, "A");

        assertThat(code).startsWith("A");
        assertThat(code).hasSize(7);
        assertThat(code.substring(1)).matches("[0-9]+");
    }

    @Test
    void generateUniqueIDNumber_withZeroLength_shouldReturnBaseLetterOnly() {
        String code = securityUtils.generateUniqueIDNumber(0, "T");

        assertThat(code).isEqualTo("T");
    }
}

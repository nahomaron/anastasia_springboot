package com.anastasia.Anastasia_BackEnd.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecurityUtils {

    public boolean hasPermission(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equalsIgnoreCase(permission));
    }

    public String generateUniqueIDNumber(int length, String baseLetter) {

        String characters = "01234456789";
        StringBuilder codeBuilder = new StringBuilder(baseLetter);
        SecureRandom secureRandom = new SecureRandom();


        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

}

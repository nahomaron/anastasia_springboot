package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;

import java.util.UUID;

/**
 * Factory for creating TenantDTO objects for API tests.
 * Generates realistic, unique, and valid payloads by default.
 */
public final class TenantDataFactory {

    private static final String DEFAULT_PASSWORD = "Password@123";

    private TenantDataFactory() {
        // Utility class
    }

    /** Generates a valid tenant (church type, monthly plan) */
    public static TenantDTO newValidTenant() {
        ChurchDTO church = ChurchDTO.builder()
                .churchName("St. " + DataGenerator.randomName() + " Church")
                .diocese("Addis Ababa")
                .email("church_" + UUID.randomUUID() + "@mail.com")
                .phone("+1408777" + (1000 + (int)(Math.random() * 8999)))
                .build();

        return TenantDTO.builder()
                .tenantType(TenantType.CHURCH)
                .subscriptionPlan(SubscriptionPlan.MONTHLY)
                .ownerName("St. " + DataGenerator.randomName() + " Church")
                .email("tenant_" + UUID.randomUUID() + "@mail.com")
                .phoneNumber("+1408555" + (1000 + (int)(Math.random() * 8999)))
                .password(DEFAULT_PASSWORD)
                .confirmPassword(DEFAULT_PASSWORD)
                .church(church)
                .build();
    }

    /** Priest tenant variant */
    public static TenantDTO newPriestTenant() {
        return TenantDTO.builder()
                .tenantType(TenantType.PRIEST)
                .subscriptionPlan(SubscriptionPlan.ANNUAL)
                .ownerName("Fr. " + DataGenerator.randomName())
                .email("priest_" + UUID.randomUUID() + "@mail.com")
                .phoneNumber("+1408666" + (1000 + (int)(Math.random() * 8999)))
                .password(DEFAULT_PASSWORD)
                .confirmPassword(DEFAULT_PASSWORD)
                .build();
    }

    /** Invalid email variant for negative testing */
    public static TenantDTO invalidEmailTenant() {
        TenantDTO dto = newValidTenant();
        dto.setEmail("not-an-email");
        return dto;
    }

    /** Missing phone variant for validation tests */
    public static TenantDTO missingPhoneTenant() {
        TenantDTO dto = newValidTenant();
        dto.setPhoneNumber(null);
        return dto;
    }

    /** Mismatched password variant */
    public static TenantDTO mismatchedPasswordTenant() {
        TenantDTO dto = newValidTenant();
        dto.setConfirmPassword("Different@123");
        return dto;
    }
}

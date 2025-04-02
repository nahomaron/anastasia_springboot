package com.anastasia.Anastasia_BackEnd.model.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType tenantType; // CHURCH or PRIEST

    @Column(nullable = false, unique = true)
    private String ownerName; // Can be a church name or a priest's full name

    @Column(nullable = false)
    private String phoneNumber; // Contact number (Church or Priest)

    private boolean isActiveTenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan; // Subscription Type

    private boolean isPaymentConfirmed; // True if payment is confirmed

}


package com.anastasia.Anastasia_BackEnd.model.tenant;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @OneToOne(mappedBy = "tenant")
    private ChurchEntity church;

    public void assignChurch(ChurchEntity church) {
        this.setChurch(church);
        church.setTenant(this);
    }

}


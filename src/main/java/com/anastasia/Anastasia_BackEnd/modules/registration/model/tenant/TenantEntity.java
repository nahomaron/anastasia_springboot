package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
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

    @Column(nullable = false)
    private String ownerName; // Can be a church name or a priest's full name

    @Column(nullable = false)
    private String phoneNumber; // Contact number (Church or Priest)

    @Builder.Default
    @Column(nullable = false)
    private boolean phoneVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActiveTenant = false;

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL)
    private ChurchEntity church;

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TenantSubscriptionEntity subscription;

    @Builder.Default
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TenantUserEntity> tenantUsers = new HashSet<>();

    public void assignChurch(ChurchEntity church) {
        this.setChurch(church);
        church.setTenant(this);
    }

    public void assignSubscription(TenantSubscriptionEntity subscription) {
        this.subscription = subscription;
        if (subscription != null) {
            subscription.setTenant(this);
        }
    }

    public void addTenantUser(TenantUserEntity tenantUser) {
        if (tenantUser == null) {
            return;
        }
        tenantUsers.add(tenantUser);
        tenantUser.setTenant(this);
    }

    public void removeTenantUser(TenantUserEntity tenantUser) {
        if (tenantUser == null) {
            return;
        }
        tenantUsers.remove(tenantUser);
        tenantUser.setTenant(null);
    }

}

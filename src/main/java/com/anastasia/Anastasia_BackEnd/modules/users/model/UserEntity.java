package com.anastasia.Anastasia_BackEnd.modules.users.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_membership", columnList = "membership_id"),
        @Index(name = "idx_user_affiliated_tenant", columnList = "affiliated_tenant_id")
})
public class UserEntity extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType = UserType.GUEST;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @OneToOne
    @JoinColumn(name = "profile_avatar_id", unique = true)
    private ImageAssetEntity profileAvatar;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ImageAssetEntity> imageAssets;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String googleId;

    private String facebookId;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_password_changed_at")
    private Instant lastPasswordChangedAt;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "temporary_password_issued_at")
    private Instant temporaryPasswordIssuedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    private String priestNumber;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private Set<Token> tokens;

    @OneToOne
    private Adult_MemberEntity membership;

    @OneToOne(mappedBy = "user")
    private StaffEntity staffProfile;

    @Column(name = "membership_id", insertable = false, updatable = false)
    private Long membershipId;

    @ManyToOne
    @JoinColumn(name = "affiliated_tenant_id")
    private TenantEntity affiliatedTenant; // Tenant the user belongs to as a church/member user

    @Column(name = "affiliated_tenant_id", insertable = false, updatable = false)
    private UUID affiliatedTenantId;

    @ManyToMany(mappedBy = "users")
    @Builder.Default
    private Set<GroupEntity> groups = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public void assignMembership(Adult_MemberEntity membership){
        this.membership = membership;
        this.membershipId = membership != null ? membership.getId() : null;
        if (membership != null) {
            membership.setUser(this);
        }
    }

    public void assignAffiliatedTenant(TenantEntity tenant){
        this.affiliatedTenant = tenant;
        this.affiliatedTenantId = tenant != null ? tenant.getId() : null;
    }

    public boolean isVerified() {
        return emailVerifiedAt != null;
    }

    public void setVerified(boolean verified) {
        if (verified) {
            if (this.emailVerifiedAt == null) {
                this.emailVerifiedAt = Instant.now();
            }
            if (this.status == null || this.status == UserStatus.PENDING_VERIFICATION) {
                this.status = UserStatus.ACTIVE;
            }
            return;
        }

        this.emailVerifiedAt = null;
        if (this.status == null || this.status == UserStatus.ACTIVE) {
            this.status = UserStatus.PENDING_VERIFICATION;
        }
    }

    public boolean isAccountLocked() {
        return status == UserStatus.LOCKED
                || (lockedUntil != null && lockedUntil.isAfter(Instant.now()));
    }

    public void setAccountLocked(boolean accountLocked) {
        if (accountLocked) {
            this.status = UserStatus.LOCKED;
            if (this.lockedAt == null) {
                this.lockedAt = Instant.now();
            }
            return;
        }

        if (this.status == UserStatus.LOCKED) {
            this.status = isVerified() ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION;
        }
        this.lockedAt = null;
        this.lockedUntil = null;
    }

    // Temporary compatibility layer while the codebase moves to affiliation naming.
    public TenantEntity getTenant() {
        return affiliatedTenant;
    }

    public void setTenant(TenantEntity tenant) {
        assignAffiliatedTenant(tenant);
    }

    public UUID getTenantId() {
        return affiliatedTenantId != null
                ? affiliatedTenantId
                : affiliatedTenant != null ? affiliatedTenant.getId() : null;
    }

    public void setTenantId(UUID tenantId) {
        this.affiliatedTenantId = tenantId;
    }

    public void assignTenant(TenantEntity tenant){
        assignAffiliatedTenant(tenant);
    }

    public void addGroup(GroupEntity group) {
        if (group != null && !this.groups.contains(group)) {
            this.groups.add(group);
            group.getUsers().add(this); // do NOT call group.addUser() again
        }
    }

}

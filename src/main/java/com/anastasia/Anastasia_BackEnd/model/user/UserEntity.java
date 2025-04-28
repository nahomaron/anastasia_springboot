package com.anastasia.Anastasia_BackEnd.model.user;

import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Auditable;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.token.Token;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", indexes = {
        @Index(name = "idx_user_membership", columnList = "membership_id"),
        @Index(name = "idx_user_tenant", columnList = "tenant_id")
})
public class UserEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String googleId;

    private String facebookId;

    private boolean accountLocked;

    private boolean verified;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Token> tokens;

    @OneToOne
    private MemberEntity membership;

    @Column(name = "membership_id", insertable = false, updatable = false)
    private Long membershipId;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant; // Linked tenant details

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private UUID tenantId;

    @ManyToMany(mappedBy = "users")
    @Builder.Default
    private Set<GroupEntity> groups = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    public void assignMembership(MemberEntity membership){
        this.setMembership(membership);
        membership.setUser(this);
    }

    public void assignTenant(TenantEntity tenant){
        this.setTenant(tenant);
    }

    public void addGroup(GroupEntity group) {
        if (group != null && !this.groups.contains(group)) {
            this.groups.add(group);
            group.getUsers().add(this); // do NOT call group.addUser() again
        }
    }

}
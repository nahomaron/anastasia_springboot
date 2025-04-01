package com.anastasia.Anastasia_BackEnd.model.entity.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class UserEntity extends Auditable {
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


    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant; // Linked tenant details

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private UUID tenantId;

}
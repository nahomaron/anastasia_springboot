package com.anastasia.Anastasia_BackEnd.model.entity.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "users")
public class UserEntity {
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @OneToMany(mappedBy = "user")
    private Set<Token> tokens;

    @ManyToOne
    @JoinColumn(name = "church_id") // Users can choose a Church later
    private ChurchEntity church;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
    private TenantEntity tenant; // Linked tenant details

    public void becomeTenant(TenantEntity tenantEntity) {
        this.tenant = tenantEntity;
        tenantEntity.setUser(this); // Set the relationship
    }
}
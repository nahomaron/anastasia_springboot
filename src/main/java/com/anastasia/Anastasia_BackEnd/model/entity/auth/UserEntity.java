package com.anastasia.Anastasia_BackEnd.model.entity.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.base.BaseEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.embeded.TenantDetails;
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

    @Embedded
    private TenantDetails tenantDetails; // Embedded TenantDetails
}
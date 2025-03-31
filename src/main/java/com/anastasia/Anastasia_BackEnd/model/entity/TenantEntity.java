package com.anastasia.Anastasia_BackEnd.model.entity;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private boolean isActiveTenant;

    private String phoneNumber;

    private String zipcode;

//    @OneToOne
//    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    private UserEntity user;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();


    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();
}

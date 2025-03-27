package com.anastasia.Anastasia_BackEnd.model.entity;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tenantId;

    private boolean isActiveTenant;

    private String phoneNumber;

    private String zipcode;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    @JsonBackReference
    private UserEntity user;
}

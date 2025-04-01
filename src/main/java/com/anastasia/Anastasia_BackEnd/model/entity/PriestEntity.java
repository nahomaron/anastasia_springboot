package com.anastasia.Anastasia_BackEnd.model.entity;

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
@Table(name = "priests")
public class PriestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant; // Only present if the priest is an independent tenant owner

    @ManyToOne
    @JoinColumn(name = "church_id")
    private ChurchEntity church;

    private String profilePicture;

    private String prefixes; //(additional title)

    private String firstName;
    private String fatherName;
    private String grandFatherName;

    private String phoneNumber;

    private String personalEmail;

    private String churchEmail;

    private String nationalIdNumber;
    private String nationality;

    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;

    private String birthdate;
    private String address;
    private String languages;
    private String levelOfEducation;

    private String roles;

}

package com.anastasia.Anastasia_BackEnd.model.priest;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
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

    @Column(unique = true, nullable = false)
    private String priestNumber;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "church_id")
    private ChurchEntity church;

    private String churchNumber;

    @OneToOne
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant; // Only present if the priest is an independent tenant owner

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriestStatus status;

    private String profilePicture;

    private String prefixes; //(additional title)

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String fatherName;

    @Column(nullable = false)
    private String grandFatherName;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    private String churchEmail;

    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;

    @Column(nullable = false)
    private String birthdate;

    private Set<String> languages;

    private String levelOfEducation;

    @Embedded
    private Address address;

    private boolean isActive;

}

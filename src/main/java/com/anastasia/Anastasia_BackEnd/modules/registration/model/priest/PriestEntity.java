package com.anastasia.Anastasia_BackEnd.modules.registration.model.priest;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private ImageAssetEntity avatar;

    @Column(nullable = false)
    private int spiritualChildren;

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

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "priest_languages", joinColumns = @JoinColumn(name = "priest_id"))
    @Column(name = "language", nullable = false, length = 128)
    private Set<String> languages = new HashSet<>();

    private String levelOfEducation;

    @Embedded
    private Address address;

    private boolean isActive;

    @Version
    @Column(nullable = false)
    private long version;

}

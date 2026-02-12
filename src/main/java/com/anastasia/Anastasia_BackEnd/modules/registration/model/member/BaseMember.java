package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseMember extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    private String membershipNumber;

    @Column(nullable = false)
    private String churchNumber;

    @Column(nullable = false)
    private String status;

    private boolean deacon;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private AvatarEntity avatar;

    private String title;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String fatherName;

    @Column(nullable = false)
    private String grandFatherName;

    @Column(nullable = false)
    private String motherName;

    @Column(nullable = false)
    private String mothersFather;

    @Column(nullable = false)
    private String firstNameT;

    @Column(nullable = false)
    private String fatherNameT;

    @Column(nullable = false)
    private String grandFatherNameT;

    @Column(nullable = false)
    private String motherFullNameT;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private LocalDate birthday;

    private String nationality;
    private String placeOfBirth;

    private String email;

    private String phone;

    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;

    private String firstLanguage;
    private String secondLanguage;

    private String levelOfEducation;

    @Column(nullable = false)
    private String fatherOfConfession;

    private String priestNumber;

    private Address address;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "church_id")
    private ChurchEntity church;

    @Column(name = "church_id", insertable = false, updatable = false)
    private Long churchId;
}

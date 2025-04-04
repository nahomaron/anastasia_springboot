package com.anastasia.Anastasia_BackEnd.model.member;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.common.Auditable;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "members")
@EntityListeners(AuditingEntityListener.class)
public class MemberEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_id_seq", allocationSize = 1)
    private Long id;

    private String membershipNumber;

    @Column(unique = true, nullable = false)
    private String churchNumber;

    @Column(nullable = false)
    private String status;

    private boolean approvedByChurch;

    private boolean approvedByPriest;

    private boolean deacon;

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
    private String birthday;

    private String nationality;
    private String placeOfBirth;

    private String email;

    @Column(nullable = false)
    private String phone;

    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;
    private String eritreaContact;

    @Column(nullable = false)
    private String maritalStatus;

    private int numberOfChildren;

    private String firstLanguage;
    private String secondLanguage;

    private String profession;
    private String levelOfEducation;

    @Column(nullable = false)
    private String fatherOfConfession;

    private String spouseIdNumber;

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

package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MemberLifecycleStatus statusValue;

    private boolean deacon;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private ImageAssetEntity avatar;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 24)
    private MemberGender genderValue;

    @Column(nullable = false)
    private LocalDate birthday;

    private String nationality;
    private String placeOfBirth;
    private String village;

    private String email;

    private String phone;

    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;

    private String firstLanguage;
    private String secondLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_of_education", length = 32)
    private EducationLevel educationLevelValue;

    @Column(nullable = false)
    private String fatherOfConfession;

    private String churchOfBaptism;
    private String baptismName;

    private String priestNumber;

    private Address address;

    @JsonIgnore
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

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "inactive_at")
    private LocalDateTime inactiveAt;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_reason", length = 512)
    private String statusReason;

    @Column(name = "consent_version", length = 64)
    private String consentVersion;

    @Column(name = "consent_accepted_at")
    private LocalDateTime consentAcceptedAt;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "source_system", length = 64)
    private String sourceSystem;

    @Column(name = "preferred_name", length = 120)
    private String preferredName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public String getStatus() {
        return statusValue != null ? statusValue.name() : null;
    }

    public void setStatus(String status) {
        this.statusValue = MemberLifecycleStatus.from(status);
    }

    public MemberLifecycleStatus getStatusEnum() {
        return statusValue;
    }

    public void setStatusEnum(MemberLifecycleStatus status) {
        this.statusValue = status;
    }

    public String getGender() {
        return genderValue != null ? genderValue.name() : null;
    }

    public void setGender(String gender) {
        this.genderValue = MemberGender.from(gender);
    }

    public MemberGender getGenderEnum() {
        return genderValue;
    }

    public void setGenderEnum(MemberGender gender) {
        this.genderValue = gender;
    }

    public String getLevelOfEducation() {
        return educationLevelValue != null ? educationLevelValue.name() : null;
    }

    public void setLevelOfEducation(String levelOfEducation) {
        this.educationLevelValue = EducationLevel.from(levelOfEducation);
    }

    public EducationLevel getEducationLevel() {
        return educationLevelValue;
    }

    public void setEducationLevel(EducationLevel educationLevel) {
        this.educationLevelValue = educationLevel;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
        this.userId = user != null ? user.getUuid() : null;
    }

    public LocalDateTime getConsentAcceptedAt() {
        return consentAcceptedAt;
    }

    public void setConsentAcceptedAt(LocalDateTime consentAcceptedAt) {
        this.consentAcceptedAt = consentAcceptedAt;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    @PrePersist
    protected void onMemberCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (registeredAt == null) {
            registeredAt = now;
        }
        if (statusChangedAt == null && statusValue != null) {
            statusChangedAt = now;
        }
    }

}

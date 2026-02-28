package com.anastasia.Anastasia_BackEnd.modules.registration.model.church;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.net.URI;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "churches")
public class ChurchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "church_seq")
    @SequenceGenerator(name = "church_seq", sequenceName = "church_id_seq", allocationSize = 1)
    private Long churchId;

    @Column(unique = true, nullable = false)
    private String churchNumber;

    @OneToOne
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    @JsonIgnore
    private TenantEntity tenant;

    @Column(nullable = false)
    private String churchName;

    @Column(nullable = false)
    private String churchNameTigrinya;

    private String prefix;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private AvatarEntity profilePicture;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private String diocese;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String denomination;

    @Column(length = 1000)
    private String description;

    private boolean usesOurServices;

    private String gpsLocation;

    private String website;

    private String instagram;

    @Column(name = "youtube_page")
    private String youtube;

    @Column(name = "facebook_page")
    private String facebook;

    @Column(name = "is_church_profile_complete", nullable = false)
    private boolean churchProfileComplete;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GroupEntity> groups;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EventEntity> events;

    @PrePersist
    @PreUpdate
    void syncProfileCompleteness() {
        this.churchProfileComplete = isComplete();
    }

    public boolean isComplete() {
        return hasText(churchName)
                && hasText(churchNameTigrinya)
                && hasText(diocese)
                && hasText(email)
                && hasText(phone)
                && hasText(description)
                && isHttpUrl(gpsLocation)
                && profilePicture != null
                && hasText(profilePicture.getImageUrl())
                && address != null
                && hasText(address.getAddressLine1())
                && hasText(address.getCity())
                && hasText(address.getCountry());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isHttpUrl(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && hasText(host);
        } catch (Exception ex) {
            return false;
        }
    }
}

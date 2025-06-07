package com.anastasia.Anastasia_BackEnd.model.church;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    private String prefix;
    private String profilePicture;

    private Address address;

    private String diocese;

    private String email;

    private String gpsLocation;

    private String websiteUrl;
    private String youtubePage;
    private String facebookPage;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupEntity> groups;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventEntity> events;
}

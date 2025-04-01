package com.anastasia.Anastasia_BackEnd.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
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
    private TenantEntity tenant;

    @Column(nullable = false)
    private String churchName;

    private String prefix;
    private String profilePicture;

    private String street;
    private String city;
    private String country;
    private String diocese;
    private String email;

    private String gpsLocation;
    private String websiteUrl;
    private String youtubePage;
    private String facebookPage;
}

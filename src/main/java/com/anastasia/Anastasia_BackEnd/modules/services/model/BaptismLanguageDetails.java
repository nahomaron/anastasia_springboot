package com.anastasia.Anastasia_BackEnd.modules.services.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class BaptismLanguageDetails {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "baptismal_name", nullable = false)
    private String baptismalName;

    @Column(name = "father_full_name", nullable = false)
    private String fatherFullName;

    @Column(name = "mother_full_name", nullable = false)
    private String motherFullName;

    @Column(name = "god_parent_full_name", nullable = false)
    private String godParentFullName;

    @Column(name = "priest_full_name", nullable = false)
    private String priestFullName;

    @Column(name = "church_of_baptism_name", nullable = false)
    private String churchOfBaptismName;
}

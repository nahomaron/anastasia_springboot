package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class MarriageIdentityInfo {

    @Column(name = "government_id_type", length = 64)
    private String governmentIdType;

    @Column(name = "government_id_number", length = 128)
    private String governmentIdNumber;

    @Column(name = "passport_number", length = 128)
    private String passportNumber;

    @Column(name = "document_number", length = 128)
    private String documentNumber;

    @Column(name = "document_expiry_date")
    private LocalDate documentExpiryDate;
}

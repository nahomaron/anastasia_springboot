package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_witnesses", indexes = {
        @Index(name = "idx_marriage_witness_case_type", columnList = "marriage_case_id, witness_type"),
        @Index(name = "idx_marriage_witness_party_type", columnList = "party_id, witness_type")
})
public class MarriageWitnessEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private MarriagePartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "witness_type", nullable = false, length = 24)
    private MarriageWitnessType witnessType;

    @Column(name = "name_english", nullable = false, length = 255)
    private String nameEnglish;

    @Column(name = "name_local", length = 255)
    private String nameLocal;

    @Column(name = "relationship_to_party", length = 128)
    private String relationshipToParty;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address_line", length = 512)
    private String addressLine;

    @Column(name = "id_type", length = 64)
    private String idType;

    @Column(name = "id_number", length = 128)
    private String idNumber;

    @Column(name = "id_document_reference", length = 1024)
    private String idDocumentReference;

    @Column(name = "testimony_completed", nullable = false)
    @Builder.Default
    private boolean testimonyCompleted = false;

    @Column(name = "testimony_date")
    private LocalDate testimonyDate;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Version
    @Column(nullable = false)
    private long version;
}

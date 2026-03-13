package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "marriage_pairing_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marriage_pairing_token_value", columnNames = {"token_value"})
        },
        indexes = {
                @Index(name = "idx_marriage_pairing_case", columnList = "marriage_case_id, active"),
                @Index(name = "idx_marriage_pairing_expires", columnList = "expires_at")
        }
)
public class MarriagePairingTokenEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_party_id")
    private MarriagePartyEntity targetParty;

    @Column(name = "token_value", nullable = false, length = 128)
    private String tokenValue;

    @Column(name = "invite_email", length = 255)
    private String inviteEmail;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "accepted_by_user_id")
    private UUID acceptedByUserId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;
}

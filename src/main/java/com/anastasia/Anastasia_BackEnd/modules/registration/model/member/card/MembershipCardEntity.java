package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membership_cards",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_membership_cards_tenant_member", columnNames = {"tenant_id", "member_id"}),
                @UniqueConstraint(name = "uk_membership_cards_card_serial", columnNames = {"card_serial_number"})
        },
        indexes = {
                @Index(name = "idx_membership_cards_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_membership_cards_membership_number", columnList = "membership_number")
        })
public class MembershipCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "membership_number", nullable = false, length = 64)
    private String membershipNumber;

    @Column(name = "member_full_name", nullable = false, length = 256)
    private String memberFullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "church_name", nullable = false, length = 256)
    private String churchName;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "card_serial_number", nullable = false, length = 64)
    private String cardSerialNumber;

    @Column(name = "qr_token_hash", nullable = false, length = 128)
    private String qrTokenHash;

    @Column(name = "qr_payload_url", nullable = false, length = 1024)
    private String qrPayloadUrl;

    @Column(name = "card_image_object_key", nullable = false, length = 512)
    private String cardImageObjectKey;

    @Column(name = "card_pdf_object_key", nullable = false, length = 512)
    private String cardPdfObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MembershipCardStatus status;

    @Column(name = "member_avatar_url", length = 1024)
    private String memberAvatarUrl;

    @Column(name = "church_logo_url", length = 1024)
    private String churchLogoUrl;

    @Column(name = "issued_by_user_id")
    private UUID issuedByUserId;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private MembershipCardTemplateEntity template;

    @Column(name = "downloaded_count", nullable = false)
    private long downloadedCount;

    @Column(name = "last_downloaded_at")
    private LocalDateTime lastDownloadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membership_card_audits", indexes = {
        @Index(name = "idx_membership_card_audits_card_time", columnList = "membership_card_id, event_time")
})
public class MembershipCardAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "membership_card_id", nullable = false)
    private MembershipCardEntity membershipCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private MembershipCardAuditEventType eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "details", length = 2048)
    private String details;

    @PrePersist
    protected void onCreate() {
        if (eventTime == null) {
            eventTime = Instant.now();
        }
    }
}

package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "membership_card_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_membership_card_templates_tenant_key", columnNames = {"tenant_id", "template_key"})
        },
        indexes = {
                @Index(name = "idx_membership_card_templates_tenant", columnList = "tenant_id")
        })
public class MembershipCardTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "church_id")
    private Long churchId;

    @Column(name = "template_key", nullable = false, length = 64)
    private String templateKey;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "primary_color", nullable = false, length = 16)
    private String primaryColor;

    @Column(name = "accent_color", nullable = false, length = 16)
    private String accentColor;

    @Column(name = "text_color", nullable = false, length = 16)
    private String textColor;

    @Column(name = "background_image_url", length = 1024)
    private String backgroundImageUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

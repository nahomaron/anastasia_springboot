package com.anastasia.Anastasia_BackEnd.core.notification.template;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_templates")
@Getter
@Setter
public class EmailTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant;

    @Column(nullable = false)
    private String name;  // e.g. "activate_account"

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    private TemplateType type = TemplateType.TENANT_CUSTOM;

    private Instant updatedAt = Instant.now();
}

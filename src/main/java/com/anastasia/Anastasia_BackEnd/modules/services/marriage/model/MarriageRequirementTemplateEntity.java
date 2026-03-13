package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
        name = "marriage_requirement_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marriage_requirement_template", columnNames = {"church_id", "code"})
        },
        indexes = {
                @Index(name = "idx_marriage_requirement_template_church", columnList = "church_id, enabled"),
                @Index(name = "idx_marriage_requirement_template_scope", columnList = "applies_to, required_flag")
        }
)
public class MarriageRequirementTemplateEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "english", column = @Column(name = "display_name_english", nullable = false, length = 255)),
            @AttributeOverride(name = "local", column = @Column(name = "display_name_local", length = 255))
    })
    private BilingualText displayName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "english", column = @Column(name = "help_text_english", length = 1000)),
            @AttributeOverride(name = "local", column = @Column(name = "help_text_local", length = 1000))
    })
    private BilingualText helpText;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 16)
    private MarriageRequirementAppliesTo appliesTo;

    @Column(name = "required_flag", nullable = false)
    @Builder.Default
    private boolean required = true;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "blocking", nullable = false)
    @Builder.Default
    private boolean blocking = true;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "condition_type", length = 128)
    private String conditionType;

    @Column(name = "document_type_association", length = 64)
    private String documentTypeAssociation;

    @Column(name = "required_count")
    private Integer requiredCount;

    @Version
    @Column(nullable = false)
    private long version;
}

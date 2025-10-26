package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "funds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_funds_tenant_name",
                columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}

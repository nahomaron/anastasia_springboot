package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

/**
 * OutboxEntity represents an event stored in the outbox table for reliable event publishing.
 */
@Entity @Table(name="outbox_events")
@Getter @Setter
public class OutboxEntity {
    @Id private UUID id;
    private String aggregateType;
    private String aggregateId;
    private UUID tenantId;
    private String type;
    @Column(name = "user_email")
    private String userEmail;
    @Column(columnDefinition="jsonb") private String payload;
    @Column(columnDefinition="jsonb") private String headers;
    private Instant createdAt;
    private boolean published;
}

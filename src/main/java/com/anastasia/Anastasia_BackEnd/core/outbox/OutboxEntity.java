package com.anastasia.Anastasia_BackEnd.core.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode headers;
    private Instant createdAt;
    private boolean published;
}

package com.anastasia.Anastasia_BackEnd.modules.groups.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "group_join_requests", indexes = {
        @Index(name = "idx_group_join_requests_group_status", columnList = "group_id,status"),
        @Index(name = "idx_group_join_requests_requester", columnList = "requester_id")
})
public class GroupJoinRequestEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserEntity requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GroupJoinRequestStatus status;

    @Column(length = 500)
    private String decisionNote;

    private Instant decidedAt;

    private UUID decidedBy;
}

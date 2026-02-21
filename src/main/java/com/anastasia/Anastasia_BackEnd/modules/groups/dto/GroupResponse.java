package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {

    private Long groupId;

    private String churchId;

    private String groupName;

    private String description;

    private String avatar;

    private String visibility;

    private Set<UUID> managers;

    private Set<UUID> users;

    private UUID createdBy;

    private UUID lastModifiedBy;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;
}

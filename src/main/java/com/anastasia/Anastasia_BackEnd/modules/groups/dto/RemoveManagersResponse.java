package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveManagersResponse {

    private String groupName;
    private int removedCount;
    private int notManagersCount;
    private int notFoundCount;
    private List<UUID> removedManagerIds;
    private List<UUID> notManagerIds;
    private List<UUID> notFoundManagerIds;
}

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
public class AddManagersResponse {

    private String groupName;
    private int addedCount;
    private int skippedCount;
    private int notFoundCount;
    private List<UUID> addedManagerIds;
    private List<UUID> skippedManagerIds;
    private List<UUID> notFoundManagerIds;
}

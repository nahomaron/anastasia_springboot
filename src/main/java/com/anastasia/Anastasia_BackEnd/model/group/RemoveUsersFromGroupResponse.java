package com.anastasia.Anastasia_BackEnd.model.group;

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
public class RemoveUsersFromGroupResponse {

    private String groupName;
    private int removedCount;
    private int notInGroupCount;
    private int notFoundCount;
    private List<UUID> removedUserIds;
    private List<UUID> notInGroupUserIds;
    private List<UUID> notFoundUserIds;
}

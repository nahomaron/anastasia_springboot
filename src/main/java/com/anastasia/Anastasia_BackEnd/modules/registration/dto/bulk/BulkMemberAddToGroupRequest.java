package com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkMemberAddToGroupRequest {

    @NotNull
    private BulkMemberTargetType memberType;

    @NotNull
    private Long groupId;

    @NotEmpty
    private Set<Long> memberIds;
}

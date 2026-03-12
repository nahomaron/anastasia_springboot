package com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkMemberActionResponse {
    private int requestedCount;
    private int matchedCount;
    private int processedCount;
    private int skippedCount;
    private int notFoundCount;
    private int missingUserCount;
    private int missingContactCount;
    private String message;
}

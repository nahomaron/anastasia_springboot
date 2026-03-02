package com.anastasia.Anastasia_BackEnd.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberFamilyItem {
    private String name;
    private String relation;
    private String status;
    private long sacramentsCompleted;
}

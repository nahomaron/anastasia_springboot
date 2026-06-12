package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportAccessSessionEndRequest {
    private String endReason;
}

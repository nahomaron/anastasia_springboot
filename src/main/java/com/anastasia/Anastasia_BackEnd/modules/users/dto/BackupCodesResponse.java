package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BackupCodesResponse {
    List<String> codes;
}

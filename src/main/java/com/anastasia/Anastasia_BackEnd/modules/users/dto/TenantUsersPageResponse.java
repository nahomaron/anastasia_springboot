package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TenantUsersPageResponse {
    private List<TenantUserRowResponse> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private List<Integer> sizeOptions;
    private List<String> roles;
    private TenantUsersMetricsResponse metrics;
}

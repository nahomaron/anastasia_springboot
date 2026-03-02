package com.anastasia.Anastasia_BackEnd.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpcomingEventItem {
    private String name;
    private LocalDate date;
    private String type;
    private String status;
}

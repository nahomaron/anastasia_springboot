package com.anastasia.Anastasia_BackEnd.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleGroupEntity {

    private Long groupId;

    private String groupName;

    private String description;
}

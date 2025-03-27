package com.anastasia.Anastasia_BackEnd.model.DTO;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {
    private Long groupId;
    private UUID tenantId;
    private Long churchId;
    private String groupName;
    private String description;
    private String avatar;
    private String visibility;
    private Set<UserDTO> managers;
}

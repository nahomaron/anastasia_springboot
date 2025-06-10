package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.seeder.TestDataSeederConfig;
import com.anastasia.Anastasia_BackEnd.service.group.GroupService;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;

import static com.anastasia.Anastasia_BackEnd.seeder.TestDataSeederConfig.TEST_EMAIL;
import static com.anastasia.Anastasia_BackEnd.seeder.TestDataSeederConfig.TEST_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
//@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestDataSeederConfig.class)
public class GroupControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GroupService groupService;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private JwtUtil jwtUtil;

    private String accessToken;
    private String tenantId;

    @BeforeEach
    void setUp() throws Exception {
        AuthenticationRequest loginRequest = new AuthenticationRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        var root = objectMapper.readTree(jsonResponse);
        accessToken = root.get("access_token").asText();
        tenantId = jwtUtil.extractTenantId(accessToken);
        TenantContext.setTenantId(UUID.fromString(tenantId));
    }

    @Test
    public void testThatCreateGroupReturns201Created() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testThatCreateGroupReturns400BadRequestOnInvalidInput() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        groupService.createGroup(groupDTO);

        GroupDTO invalidGroupDTO = GroupDTO.builder()
                .churchId("C12343434")
                .groupName("Integration Test Group")
                .visibility("Public")
                .build();
        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidGroupDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testThatListOfGroupsReturns200OkWithPageOfGroups() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        groupService.createGroup(groupDTO);

        mockMvc.perform(get("/api/v1/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.groupDTOList[0].groupName").value(groupDTO.getGroupName()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    public void testThatGetGroupReturns200OkWhenGroupExists() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        SimpleGroupEntity created = groupService.createGroup(groupDTO);

        mockMvc.perform(get("/api/v1/groups/" + created.getGroupId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isFound())
                .andExpect(jsonPath("$.groupName").value(groupDTO.getGroupName()));
    }

    @Test
    public void testThatGetGroupReturns404NotFoundWhenGroupDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/groups/999999")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testThatUpdateGroupReturns202AcceptedOnSuccess() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        SimpleGroupEntity created = groupService.createGroup(groupDTO);
        groupDTO.setDescription("Updated description");

        mockMvc.perform(put("/api/v1/groups/" + created.getGroupId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupDTO)))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testThatUpdateGroupReturns404NotFoundWhenGroupDoesNotExist() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        mockMvc.perform(put("/api/v1/groups/99999")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testThatDeleteGroupReturns204NoContentOnSuccess() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        SimpleGroupEntity created = groupService.createGroup(groupDTO);

        mockMvc.perform(delete("/api/v1/groups/" + created.getGroupId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testThatAddUsersToGroupReturns200Ok() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        SimpleGroupEntity created = groupService.createGroup(groupDTO);

        AddUsersToGroupRequest request = AddUsersToGroupRequest.builder()
                .userIds(Set.of(TestDataSeederConfig.TEST_USER_UUID))
                .build();

        mockMvc.perform(post("/api/v1/groups/" + created.getGroupId() + "/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedCount").value(1));
    }

    @Test
    public void testThatListGroupMembersReturns200Ok() throws Exception {
        GroupDTO groupDTO = getValidGroupDTO();
        SimpleGroupEntity created = groupService.createGroup(groupDTO);

        AddUsersToGroupRequest request = AddUsersToGroupRequest.builder()
                .userIds(Set.of(TestDataSeederConfig.TEST_USER_UUID))
                .build();
        groupService.addUsersToGroup(created.getGroupId(), request);

        mockMvc.perform(get("/api/v1/groups/group/" + created.getGroupId() + "/members")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.simpleUserDTOList[0].uuid").value(TestDataSeederConfig.TEST_USER_UUID.toString()));
    }

    private GroupDTO getValidGroupDTO() {
        UUID tenantContextId = TenantContext.getTenantId();
        ChurchEntity church = churchRepository.findByTenantId(tenantContextId)
                .orElseThrow(() -> new EntityNotFoundException("No church assigned to tenant"));
        return TestDataUtil.createTestGroupDTO(String.valueOf(church.getChurchId()));
    }
}

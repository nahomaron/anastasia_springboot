package com.anastasia.Anastasia_BackEnd.UnitTests.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupDTO;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupResponse;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class GroupMapperUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private GroupMapper groupMapper;

    private UserEntity manager;
    private UserEntity member;

    @BeforeEach
    void setUp() {
        manager = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .fullName("Manager One")
                .email("manager@example.com")
                .build();

        member = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .fullName("Member One")
                .email("member@example.com")
                .build();
    }

    @Test
    void groupEntityToResponse_shouldMapAllFields() {
        ChurchEntity church = ChurchEntity.builder()
                .churchId(10L)
                .churchNumber("CH100")
                .churchName("St. Anastasia")
                .build();

        GroupEntity entity = GroupEntity.builder()
                .groupId(55L)
                .tenantId(UUID.randomUUID())
                .church(church)
                .groupName("Youth Group")
                .description("Weekly youth gathering")
                .avatar("avatar.png")
                .visibility("PUBLIC")
                .managers(Set.of(manager))
                .users(Set.of(member))
                .build();

        GroupResponse dto = groupMapper.groupEntityToResponse(entity);

        assertThat(dto.getChurchId()).isEqualTo(church.getChurchId().toString());
        assertThat(dto.getGroupName()).isEqualTo("Youth Group");
        assertThat(dto.getDescription()).isEqualTo("Weekly youth gathering");
        assertThat(dto.getAvatar()).isEqualTo("avatar.png");
        assertThat(dto.getVisibility()).isEqualTo("PUBLIC");
        assertThat(dto.getManagers()).containsExactly(manager.getUuid());
        assertThat(dto.getUsers()).containsExactly(member.getUuid());
    }

    @Test
    void groupEntityToResponse_withNullEntity_returnsNull() {
        assertThat(groupMapper.groupEntityToResponse(null)).isNull();
    }

    @Test
    void groupDTOToEntity_shouldResolveUsersAndManagers() {
        UUID managerId = manager.getUuid();
        UUID memberId = member.getUuid();
        when(userService.findEntity(eq(managerId))).thenReturn(Optional.of(manager));
        when(userService.findEntity(eq(memberId))).thenReturn(Optional.of(member));

        GroupDTO dto = GroupDTO.builder()
                .churchId("15")
                .groupName("Choir")
                .description("Choir practice")
                .avatar("choir.png")
                .visibility("PRIVATE")
                .managers(Set.of(managerId))
                .users(Set.of(memberId))
                .build();

        GroupEntity entity = groupMapper.groupDTOToEntity(dto);

        assertNotNull(entity);
        assertEquals("Choir", entity.getGroupName());
        assertEquals("Choir practice", entity.getDescription());
        assertEquals("choir.png", entity.getAvatar());
        assertEquals("PRIVATE", entity.getVisibility());
        assertNotNull(entity.getTenantId(), "Tenant id should be generated");
        assertThat(entity.getManagers()).containsExactly(manager);
        assertThat(entity.getUsers()).containsExactly(member);
    }

    @Test
    void groupDTOToEntity_whenUserMissing_throws() {
        UUID missingId = UUID.randomUUID();
        when(userService.findEntity(eq(missingId))).thenReturn(Optional.empty());

        GroupDTO dto = GroupDTO.builder()
                .churchId("15")
                .groupName("Choir")
                .visibility("PRIVATE")
                .users(Set.of(missingId))
                .build();

        assertThrows(EntityNotFoundException.class, () -> groupMapper.groupDTOToEntity(dto));
    }

    @Test
    void groupDTOToEntity_withNullDTO_returnsNull() {
        assertThat(groupMapper.groupDTOToEntity(null)).isNull();
    }
}

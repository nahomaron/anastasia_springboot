package com.anastasia.Anastasia_BackEnd.IntegrationTest.repository;

import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.testsupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Repository Layer - UserRepository")
class UserRepositoryIT extends ServiceIntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;

    @Test
    void userRepository_resolvesCustomQueries() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        Role adminRole = fetchRole(RoleType.ADMIN);

        UserEntity manager = persistUser("repo-manager+" + UUID.randomUUID() + "@integration.com", ownerRole);
        UserEntity member = persistUser("repo-member+" + UUID.randomUUID() + "@integration.com", adminRole);

        GroupEntity group = GroupEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .groupName("Repository Group")
                .visibility("public")
                .description("Repo test")
                .build();
        group.getManagers().add(manager);
        group.addUser(member);
        groupRepository.save(group);

        Page<SimpleUserDTO> groupMembers = userRepository.findUsersByGroupId(group.getGroupId(), PageRequest.of(0, 5));
        assertThat(groupMembers.getContent()).extracting(SimpleUserDTO::uuid).contains(member.getUuid());

        assertThat(userRepository.findAllByEmailIn(Set.of(member.getEmail()))).hasSize(1);

        UserEntity tenantAdmin = userRepository.save(UserEntity.builder()
                .fullName("Tenant Admin")
                .email("tenant-admin+" + UUID.randomUUID() + "@integration.com")
                .password("Secret123!")
                .tenant(tenant)
                .userType(UserType.TENANT)
                .verified(true)
                .roles(Set.of(ownerRole))
                .build());

        assertThat(userRepository.findTenantAdmin(tenant.getId()))
                .map(UserEntity::getEmail)
                .contains(tenantAdmin.getEmail());
    }
}

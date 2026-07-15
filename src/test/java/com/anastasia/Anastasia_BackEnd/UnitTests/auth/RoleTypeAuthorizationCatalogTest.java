package com.anastasia.Anastasia_BackEnd.UnitTests.auth;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTypeAuthorizationCatalogTest {

    @Test
    void platformAdmin_shouldCarryTheFullPermissionCatalog() {
        assertThat(RoleType.PLATFORM_ADMIN.getPermissions())
                .containsExactlyInAnyOrderElementsOf(EnumSet.allOf(PermissionType.class));
    }

    @Test
    void primaryAdmin_shouldCarryGranularTenantPermissions() {
        assertThat(RoleType.PRIMARY_ADMIN.getPermissions())
                .contains(
                        PermissionType.VIEW_MEMBERS,
                        PermissionType.ADD_MEMBERS,
                        PermissionType.VIEW_GROUPS,
                        PermissionType.DELETE_GROUPS,
                        PermissionType.VIEW_EVENTS,
                        PermissionType.CREATE_EDIT_EVENTS,
                        PermissionType.VIEW_CALENDAR,
                        PermissionType.MANAGE_CALENDAR,
                        PermissionType.VIEW_APPOINTMENTS,
                        PermissionType.VIEW_FINANCE_REPORT,
                        PermissionType.GENERATE_FINANCE_REPORT
                )
                .doesNotContain(PermissionType.MANAGE_TENANTS, PermissionType.VIEW_ALL_DATA, PermissionType.OWN_SUBSCRIPTION);
    }

    @Test
    void priest_shouldCarryReadPermissionsNeededByPriestFacingControllers() {
        assertThat(RoleType.PRIEST.getPermissions())
                .contains(
                        PermissionType.VIEW_MEMBERS,
                        PermissionType.VIEW_CHILDREN,
                        PermissionType.VIEW_GROUPS,
                        PermissionType.VIEW_EVENTS,
                        PermissionType.VIEW_CALENDAR,
                        PermissionType.VIEW_APPOINTMENTS,
                        PermissionType.VIEW_PRIESTS,
                        PermissionType.VIEW_PRIEST_ASSIGNMENTS
                )
                .doesNotContain(PermissionType.MANAGE_CALENDAR, PermissionType.DELETE_GROUPS);
    }
}

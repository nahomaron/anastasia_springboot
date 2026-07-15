package com.anastasia.Anastasia_BackEnd.core.auth.role;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType.*;

@Getter
@RequiredArgsConstructor
public enum RoleType {

    PLATFORM_ADMIN(
            Set.copyOf(java.util.EnumSet.allOf(PermissionType.class)),
            "Developer with full platform access"
    ),

    OWNER(
            Set.of(
                    OWN_SUBSCRIPTION, MANAGE_TENANT_BILLING
            ), "Owns the subscription"
    ),

    PRIMARY_ADMIN(
            Set.of(
                    MANAGE_USERS, VIEW_TENANT_USERS, INVITE_TENANT_USERS, MANAGE_TENANT_USERS,
                    MANAGE_ROLES, MANAGE_TENANT_BILLING, MANAGE_MEMBERS, MANAGE_GROUPS,
                    MANAGE_EVENTS, VIEW_CALENDAR, MANAGE_CALENDAR, MANAGE_FOLLOWUPS, MANAGE_APPOINTMENT, VIEW_APPOINTMENTS, MANAGE_FINANCE,
                    VIEW_MEMBERS, ADD_MEMBERS, EDIT_MEMBERS, DELETE_MEMBERS, SMS_MEMBERS,
                    EMAIL_MEMBERS, NOTIFY_MEMBERS, COMMUNICATE_WITH_PARENTS, COMMUNICATE_WITH_CHILDREN,
                    ADVANCED_SEARCH_MEMBERS, APPROVE_MEMBERSHIP, ADD_EDIT_MEMBER_REPORTS, VIEW_MEMBER_REPORTS,
                    VIEW_CHILDREN, EDIT_CHILDREN, DELETE_CHILDREN,
                    VIEW_GROUPS, CREATE_GROUPS, EDIT_GROUPS, ADD_MEMBERS_TO_GROUPS, REMOVE_MEMBERS_FROM_GROUPS,
                    DELETE_GROUPS, MANAGE_REQUESTS,
                    CREATE_EDIT_EVENTS, VIEW_EVENTS, DELETE_EVENTS, OPEN_EVENT_REGISTRATION, CLOSE_EVENT_REGISTRATION,
                    REGISTER_PEOPLE, VIEW_EVENT_REPORTS, CHECK_IN_ATTENDANCE, CHECK_OUT_ATTENDANCE, MARK_ATTENDANCE,
                    VIEW_FOLLOWUP_ASSIGNED_TO_ME, VIEW_FOLLOWUP_ASSIGNED_BY_ME, VIEW_FOLLOWUP_ASSIGNED_TO_OTHERS,
                    ADD_FOLLOWUP, EDIT_FOLLOWUP, DELETE_FOLLOWUP, MODIFY_FOLLOWUP_ACTIONS,
                    BOOK_APPOINTMENT, CANCEL_APPOINTMENT,
                    VIEW_ACCOUNTS, MANAGE_ACCOUNTS, VIEW_FUNDS, MANAGE_FUNDS, RECORD_TRANSACTIONS,
                    RECONCILE_ACCOUNTS, IMPORT_FINANCIAL_DATA, EXPORT_FINANCIAL_DATA,
                    VIEW_FINANCE_REPORT, GENERATE_FINANCE_REPORT, MANAGE_DONATIONS, VIEW_DONATION_REPORTS,
                    GENERATE_DONATION_RECEIPTS,
                    VIEW_SERVICES, MANAGE_SERVICES, STREAM_SERVICES,
                    VIEW_STAFF, MANAGE_STAFF, RESET_STAFF_CREDENTIALS,
                    VIEW_VOLUNTEERS, MANAGE_VOLUNTEERS, SCHEDULE_VOLUNTEERS,
                    VIEW_PRAYER_REQUESTS, MANAGE_PRAYER_REQUESTS, ASSIGN_PASTORAL_VISITS,
                    VIEW_SUNDAY_SCHOOL_CLASSES, MANAGE_SUNDAY_SCHOOL, ASSIGN_TEACHERS,
                    BOOK_FACILITIES, MANAGE_CHURCH_RESOURCES,
                    SEND_ANNOUNCEMENTS, MANAGE_BULLETINS,
                    VIEW_PRIESTS, MANAGE_PRIESTS, VIEW_PRIEST_ASSIGNMENTS,
                    APPROVE_MEMBERSHIP_AS_PRIEST, VIEW_PRIEST_DASHBOARD
            ), "Primary tenant admin has full tenant-level permissions except platform-only capabilities"
    ),

    USER(
            Collections.emptySet(),
            "User does not have roles"
    ),
    MEMBER(
            Collections.emptySet(),
            "Member access"
    ),
    STAFF(
            Collections.emptySet(),
            "Operational staff access"
    ),

    ADMIN(
            Set.of(
                    VIEW_TENANT_USERS
            ), "Admin starts with limited default access and should be granted explicit permissions as needed"
    ),
    PRIEST(
            Set.of(
                    VIEW_MEMBERS, ADD_MEMBERS, EDIT_MEMBERS, DELETE_MEMBERS, SMS_MEMBERS,
                    VIEW_CHILDREN,
                    VIEW_GROUPS, VIEW_EVENTS, MANAGE_REQUESTS, VIEW_STAFF, VIEW_CALENDAR,
                    VIEW_APPOINTMENTS,
                    VIEW_PRIESTS, VIEW_PRIEST_ASSIGNMENTS,
                    APPROVE_MEMBERSHIP_AS_PRIEST, VIEW_PRIEST_DASHBOARD,
                    ADVANCED_SEARCH_MEMBERS
            ), "Priest has role of pastoring members"
    )

    ;
    private final Set<PermissionType> permissions;

    private final String description;

    public List<SimpleGrantedAuthority> getAuthorities(){
        var authorities = new ArrayList<>(getPermissions().stream()
                .map(permissionType -> new SimpleGrantedAuthority(permissionType.name()))
                .toList());

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return authorities;
    }

}

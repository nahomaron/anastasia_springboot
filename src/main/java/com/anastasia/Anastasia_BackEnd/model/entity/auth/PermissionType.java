package com.anastasia.Anastasia_BackEnd.model.entity.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionType {

    // User & Role Management
    MANAGE_USERS("manage_users", "Can create, update, and delete users"),
    MANAGE_ROLES("manage_roles", "Can create, update, and delete roles"),

    // Member Management
    VIEW_MEMBERS("view_members", "Can view member profiles"),
    ADD_MEMBERS("add_members", "Can add new members"),
    EDIT_MEMBERS("edit_members", "Can update member details"),
    DELETE_MEMBERS("delete_members", "Can remove members"),
    SMS_MEMBERS("sms_members", "Can send SMS to members"),
    EMAIL_MEMBERS("email_members", "Can send emails to members"),
    NOTIFY_MEMBERS("notify_members", "Can send notifications to members"),
    COMMUNICATE_WITH_PARENTS("communicate_with_parents", "Can contact parents of child members"),
    COMMUNICATE_WITH_CHILDREN("communicate_with_children", "Can contact child members"),
    ADVANCED_SEARCH_MEMBERS("advanced_search_members", "Can perform advanced searches on members"),
    APPROVE_MEMBERSHIP("approve_membership", "Can approve or reject membership applications"),
    ADD_EDIT_MEMBER_REPORTS("add_edit_member_reports", "Can create and edit member reports"),
    VIEW_MEMBER_REPORTS("view_member_reports", "Can view member reports"),

    // Group Management
    VIEW_GROUPS("view_groups", "Can view groups"),
    CREATE_GROUPS("create_groups", "Can create new groups"),
    EDIT_GROUPS("edit_groups", "Can edit existing groups"),
    ADD_MEMBERS_TO_GROUPS("add_members_to_groups", "Can add members to groups"),
    REMOVE_MEMBERS_FROM_GROUPS("remove_members_from_groups", "Can remove members from groups"),
    MANAGE_REQUESTS("manage_requests", "Can manage group membership requests"),

    // Event Management
    CREATE_EDIT_EVENTS("create_edit_events", "Can create and edit events"),
    VIEW_EVENTS("view_events", "Can view events"),
    DELETE_EVENTS("delete_events", "Can delete events"),
    OPEN_EVENT_REGISTRATION("open_event_registration", "Can open registration for events"),
    CLOSE_EVENT_REGISTRATION("close_event_registration", "Can close registration for events"),
    REGISTER_PEOPLE("register_people", "Can register attendees for events"),
    VIEW_EVENT_REPORTS("view_event_reports", "Can view event attendance reports"),
    CHECK_IN_ATTENDANCE("check_in_attendance", "Can check in attendees at events"),
    CHECK_OUT_ATTENDANCE("check_out_attendance", "Can check out attendees at events"),

    // Follow-ups
    VIEW_FOLLOWUP_ASSIGNED_TO_ME("view_followup_assigned_to_me", "Can view follow-ups assigned to self"),
    VIEW_FOLLOWUP_ASSIGNED_BY_ME("view_followup_assigned_by_me", "Can view follow-ups assigned by self"),
    VIEW_FOLLOWUP_ASSIGNED_TO_OTHERS("view_followup_assigned_to_others", "Can view follow-ups assigned to others"),
    ADD_FOLLOWUP("add_followup", "Can create follow-up tasks"),
    EDIT_FOLLOWUP("edit_followup", "Can edit follow-up tasks"),
    DELETE_FOLLOWUP("delete_followup", "Can delete follow-up tasks"),
    MODIFY_FOLLOWUP_ACTIONS("modify_followup_actions", "Can modify follow-up actions"),

    // Appointments
    BOOK_APPOINTMENT("book_appointment", "Can book appointments"),
    CANCEL_APPOINTMENT("cancel_appointment", "Can cancel appointments"),

    // Finance Management
    VIEW_FINANCE_REPORT("view_finance_report", "Can view financial reports"),
    GENERATE_FINANCE_REPORT("generate_finance_report", "Can generate financial reports"),
    MANAGE_DONATIONS("manage_donations", "Can record, edit, and delete donations"),
    VIEW_DONATION_REPORTS("view_donation_reports", "Can view donation reports"),
    GENERATE_DONATION_RECEIPTS("generate_donation_receipts", "Can generate donation receipts"),

    // Worship & Services
    VIEW_SERVICES("view_services", "Can view worship schedules"),
    MANAGE_SERVICES("manage_services", "Can create, edit, and delete worship services"),
    STREAM_SERVICES("stream_services", "Can manage live-streaming of worship services"),

    // Volunteer & Staff Management
    VIEW_VOLUNTEERS("view_volunteers", "Can view volunteer lists"),
    MANAGE_VOLUNTEERS("manage_volunteers", "Can assign and remove volunteers"),
    SCHEDULE_VOLUNTEERS("schedule_volunteers", "Can schedule volunteer tasks"),

    // Prayer Requests & Pastoral Care
    VIEW_PRAYER_REQUESTS("view_prayer_requests", "Can view prayer requests"),
    MANAGE_PRAYER_REQUESTS("manage_prayer_requests", "Can approve or delete prayer requests"),
    ASSIGN_PASTORAL_VISITS("assign_pastoral_visits", "Can assign pastors for home/hospital visits"),

    // Sunday School & Youth Ministry
    VIEW_SUNDAY_SCHOOL_CLASSES("view_sunday_school_classes", "Can view Sunday school class schedules"),
    MANAGE_SUNDAY_SCHOOL("manage_sunday_school", "Can create, edit, and delete Sunday school classes"),
    ASSIGN_TEACHERS("assign_teachers", "Can assign teachers to Sunday school classes"),

    // Resource & Facility Management
    BOOK_FACILITIES("book_facilities", "Can reserve church facilities for events"),
    MANAGE_CHURCH_RESOURCES("manage_church_resources", "Can manage church assets like projectors, chairs, and books"),

    // Communications & Announcements
    SEND_ANNOUNCEMENTS("send_announcements", "Can send church-wide notifications"),
    MANAGE_BULLETINS("manage_bulletins", "Can create and edit church bulletins/newsletters");

    private final String name;
    private final String description;
}

package com.anastasia.Anastasia_BackEnd.core.notification.template;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EmailTemplate {

    WELCOME_USER(
            "email/scenarios/auth/welcome-user",
            "email.subject.welcome-user",
            EmailAudienceType.USER,
            EmailCategory.AUTH,
            List.of("userName", "churchName", "loginUrl", "supportEmail"),
            List.of("churchName", "appName"),
            "Send once when account is created and email is verified.",
            key -> new Object[]{key.getOrDefault("churchName", "Your Church"), key.getOrDefault("appName", "Anastasia")}
    ),
    VERIFY_EMAIL_OTP(
            "email/scenarios/auth/verify-email-otp",
            "email.subject.verify-email-otp",
            EmailAudienceType.USER,
            EmailCategory.SECURITY,
            List.of("userName", "code", "expiresMinutes", "helpUrl"),
            List.of("appName"),
            "Send on verification request; rate-limit by user and IP.",
            key -> new Object[]{key.getOrDefault("appName", "Anastasia")}
    ),
    VERIFY_EMAIL_LINK(
            "email/scenarios/auth/verify-email-link",
            "email.subject.verify-email-link",
            EmailAudienceType.USER,
            EmailCategory.SECURITY,
            List.of("userName", "verifyUrl", "expiresHours"),
            List.of("appName"),
            "Send on verification-link flows; single active token at a time.",
            key -> new Object[]{key.getOrDefault("appName", "Anastasia")}
    ),
    PASSWORD_RESET(
            "email/scenarios/auth/password-reset",
            "email.subject.password-reset",
            EmailAudienceType.USER,
            EmailCategory.SECURITY,
            List.of("userName", "resetUrl", "expiresHours"),
            List.of("appName"),
            "Send after reset request; throttle repeated requests per user.",
            key -> new Object[]{key.getOrDefault("appName", "Anastasia")}
    ),
    TENANT_CREATED(
            "email/scenarios/onboarding/tenant-created",
            "email.subject.tenant-created",
            EmailAudienceType.OWNER,
            EmailCategory.TENANT,
            List.of("ownerName", "churchName", "dashboardUrl", "nextStepsUrl"),
            List.of(),
            "Send once after successful tenant provisioning.",
            key -> new Object[]{}
    ),
    NEW_MEMBER_REGISTERED(
            "email/scenarios/member/new-member-registered",
            "email.subject.new-member-registered",
            EmailAudienceType.MIXED,
            EmailCategory.ADMIN_ALERT,
            List.of("memberName", "memberEmail", "memberPhone", "memberId", "reviewUrl"),
            List.of("memberName"),
            "Send to admins/priests immediately after member submission; dedupe by memberId.",
            key -> new Object[]{key.getOrDefault("memberName", "Member")}
    ),
    MEMBER_APPROVED(
            "email/scenarios/member/member-approved",
            "email.subject.member-approved",
            EmailAudienceType.MEMBER,
            EmailCategory.MEMBER,
            List.of("memberName", "churchName", "portalUrl"),
            List.of("churchName"),
            "Send when member status transitions to approved.",
            key -> new Object[]{key.getOrDefault("churchName", "Your Church")}
    ),
    EVENT_INVITATION(
            "email/scenarios/events/event-invitation",
            "email.subject.event-invitation",
            EmailAudienceType.MEMBER,
            EmailCategory.EVENT,
            List.of("memberName", "eventTitle", "date", "time", "location", "eventUrl", "rsvpUrl"),
            List.of("eventTitle"),
            "Send at invitation creation; avoid duplicates per event/member pair.",
            key -> new Object[]{key.getOrDefault("eventTitle", "Church Event")}
    ),
    EVENT_REMINDER(
            "email/scenarios/events/event-reminder",
            "email.subject.event-reminder",
            EmailAudienceType.MEMBER,
            EmailCategory.EVENT,
            List.of("memberName", "eventTitle", "date", "time", "location", "eventUrl"),
            List.of("eventTitle", "date"),
            "Send 24h and optionally 1h before event start with idempotency per reminder window.",
            key -> new Object[]{key.getOrDefault("eventTitle", "Church Event"), key.getOrDefault("date", "")}
    ),
    APPOINTMENT_REQUESTED(
            "email/scenarios/appointments/appointment-requested",
            "email.subject.appointment-requested",
            EmailAudienceType.PRIEST,
            EmailCategory.APPOINTMENT,
            List.of("memberName", "requestedTimes", "purpose", "memberContact", "approveUrl", "declineUrl"),
            List.of("memberName"),
            "Send to assigned priest/admin when request is created; dedupe by appointment id.",
            key -> new Object[]{key.getOrDefault("memberName", "Member")}
    ),
    APPOINTMENT_CONFIRMED(
            "email/scenarios/appointments/appointment-confirmed",
            "email.subject.appointment-confirmed",
            EmailAudienceType.MEMBER,
            EmailCategory.APPOINTMENT,
            List.of("memberName", "dateTime", "locationOrMeetingLink", "cancelUrl", "rescheduleUrl"),
            List.of(),
            "Send after appointment confirmation.",
            key -> new Object[]{}
    ),
    FORM_SUBMITTED(
            "email/scenarios/forms/form-submitted",
            "email.subject.form-submitted",
            EmailAudienceType.MEMBER,
            EmailCategory.FORM,
            List.of("memberName", "formName", "submittedAt", "trackingId", "portalUrl"),
            List.of("formName"),
            "Send to submitter immediately after receiving form.",
            key -> new Object[]{key.getOrDefault("formName", "request")}
    ),
    FORM_APPROVED(
            "email/scenarios/forms/form-approved",
            "email.subject.form-approved",
            EmailAudienceType.MEMBER,
            EmailCategory.FORM,
            List.of("memberName", "formName", "approvedAt", "portalUrl"),
            List.of("formName"),
            "Send when form status changes to approved.",
            key -> new Object[]{key.getOrDefault("formName", "request")}
    ),
    ADMIN_DIGEST(
            "email/scenarios/digest/admin-digest",
            "email.subject.admin-digest",
            EmailAudienceType.ADMIN,
            EmailCategory.ADMIN_ALERT,
            List.of("newMembersCount", "pendingFormsCount", "upcomingEventsCount", "dashboardUrl"),
            List.of(),
            "Send weekly at a fixed tenant-local schedule with one digest per tenant window.",
            key -> new Object[]{}
    );

    private static final Map<String, EmailTemplate> BY_TEMPLATE_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(EmailTemplate::templateKey, Function.identity()));

    private final String templateKey;
    private final String subjectKey;
    private final EmailAudienceType audienceType;
    private final EmailCategory category;
    private final List<String> requiredVars;
    private final List<String> subjectArgVars;
    private final String sendingRules;
    private final Function<Map<String, Object>, Object[]> subjectArgsProvider;

    EmailTemplate(String templateKey,
                  String subjectKey,
                  EmailAudienceType audienceType,
                  EmailCategory category,
                  List<String> requiredVars,
                  List<String> subjectArgVars,
                  String sendingRules,
                  Function<Map<String, Object>, Object[]> subjectArgsProvider) {
        this.templateKey = templateKey;
        this.subjectKey = subjectKey;
        this.audienceType = audienceType;
        this.category = category;
        this.requiredVars = requiredVars;
        this.subjectArgVars = subjectArgVars;
        this.sendingRules = sendingRules;
        this.subjectArgsProvider = subjectArgsProvider;
    }

    public String templateKey() {
        return templateKey;
    }

    public String subjectKey() {
        return subjectKey;
    }

    public EmailAudienceType audienceType() {
        return audienceType;
    }

    public EmailCategory category() {
        return category;
    }

    public List<String> requiredVars() {
        return requiredVars;
    }

    public List<String> subjectArgVars() {
        return subjectArgVars;
    }

    public String sendingRules() {
        return sendingRules;
    }

    public Object[] subjectArgs(Map<String, Object> model) {
        Map<String, Object> safeModel = model == null ? Map.of() : model;
        return subjectArgsProvider.apply(safeModel);
    }

    public static EmailTemplate fromTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            throw new IllegalArgumentException("Template key is required");
        }
        EmailTemplate template = BY_TEMPLATE_KEY.get(templateKey);
        if (template == null) {
            throw new IllegalArgumentException("Unknown email template: " + templateKey);
        }
        return template;
    }

    public static EmailTemplate fromAnyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Template key is required");
        }

        EmailTemplate byTemplate = BY_TEMPLATE_KEY.get(key);
        if (byTemplate != null) {
            return byTemplate;
        }

        String enumName = key.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        for (EmailTemplate value : values()) {
            if (value.name().equals(enumName)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown email template: " + key);
    }

    public Set<String> missingRequiredVars(Map<String, Object> model) {
        Map<String, Object> safe = model == null ? Map.of() : model;
        return requiredVars.stream()
                .filter(key -> {
                    Object value = safe.get(key);
                    return value == null || (value instanceof String s && s.isBlank());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

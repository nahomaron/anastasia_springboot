package com.anastasia.Anastasia_BackEnd.core.notification.controller;

import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/dev/email")
public class DevEmailPreviewController {

    private final EmailTemplateService emailTemplateService;

    public DevEmailPreviewController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@RequestParam("template") String templateKey) {
        EmailTemplate template = EmailTemplate.fromAnyKey(templateKey);
        Map<String, Object> model = sampleModel(template);
        String html = emailTemplateService.renderHtml(template.templateKey(), model);
        return ResponseEntity.ok(html);
    }

    private Map<String, Object> sampleModel(EmailTemplate template) {
        Map<String, Object> common = new LinkedHashMap<>();
        common.put("appName", "Anastasia");
        common.put("churchName", "St. Raphael Cathedral");
        common.put("appLogoUrl", "https://dummyimage.com/120x32/3b5bff/ffffff.png&text=Anastasia");
        common.put("churchLogoUrl", "https://dummyimage.com/140x36/f6f7fb/3b5bff.png&text=St.+Raphael");
        common.put("supportEmail", "support@anastasia.app");
        common.put("userName", "Mariam");
        common.put("memberName", "Mariam Salib");
        common.put("helpUrl", "https://app.anastasia.com/help/security");
        common.put("requestIp", "203.0.113.11");
        common.put("requestDevice", "Chrome on macOS");
        common.put("primaryCtaUrl", "https://app.anastasia.com");
        common.put("secondaryCtaUrl", "https://app.anastasia.com/help");
        common.put("footerAddress", "12 Church Street, Boston, MA");

        switch (template) {
            case WELCOME_USER -> {
                common.put("loginUrl", "https://app.anastasia.com/login");
            }
            case VERIFY_EMAIL_OTP -> {
                common.put("code", "349281");
                common.put("expiresMinutes", 10);
            }
            case VERIFY_EMAIL_LINK -> {
                common.put("verifyUrl", "https://app.anastasia.com/verify?token=abc123");
                common.put("expiresHours", 24);
            }
            case PASSWORD_RESET -> {
                common.put("resetUrl", "https://app.anastasia.com/reset?token=abc123");
                common.put("expiresHours", 1);
            }
            case TENANT_CREATED -> {
                common.put("ownerName", "Fr. Daniel");
                common.put("dashboardUrl", "https://app.anastasia.com/dashboard");
                common.put("nextStepsUrl", "https://app.anastasia.com/onboarding");
            }
            case NEW_MEMBER_REGISTERED -> {
                common.put("memberEmail", "member@example.com");
                common.put("memberPhone", "+1 202-555-0188");
                common.put("memberId", "M-10482");
                common.put("reviewUrl", "https://app.anastasia.com/admin/members/M-10482");
            }
            case MEMBER_APPROVED -> {
                common.put("portalUrl", "https://app.anastasia.com/portal");
            }
            case ADMIN_MEMBER_NOTIFICATION -> {
                common.put("emailSubject", "A message from St. Raphael Cathedral");
                common.put("messageTitle", "Liturgy schedule update");
                common.put("messageContent", "Peace be with you.\n\nPlease arrive 20 minutes early for Sunday's service.\n\nThank you.");
            }
            case EVENT_INVITATION, EVENT_REMINDER -> {
                common.put("eventTitle", "Young Families Fellowship");
                common.put("date", "Sunday, March 15, 2026");
                common.put("time", "6:30 PM");
                common.put("location", "Parish Hall");
                common.put("eventUrl", "https://app.anastasia.com/events/884");
                common.put("icsUrl", "https://app.anastasia.com/events/884/calendar.ics");
                common.put("rsvpUrl", "https://app.anastasia.com/events/884/rsvp");
            }
            case APPOINTMENT_REQUESTED -> {
                common.put("requestedTimes", "Tue 10:00 AM, Wed 3:00 PM");
                common.put("purpose", "Spiritual counseling");
                common.put("memberContact", "member@example.com | +1 202-555-0118");
                common.put("approveUrl", "https://app.anastasia.com/appointments/332/approve");
                common.put("declineUrl", "https://app.anastasia.com/appointments/332/decline");
            }
            case APPOINTMENT_CONFIRMED -> {
                common.put("dateTime", "Tuesday, March 10, 2026 at 10:00 AM");
                common.put("locationOrMeetingLink", "Church Office - Room 2");
                common.put("cancelUrl", "https://app.anastasia.com/appointments/332/cancel");
                common.put("rescheduleUrl", "https://app.anastasia.com/appointments/332/reschedule");
            }
            case FORM_SUBMITTED -> {
                common.put("formName", "Baptism Request");
                common.put("submittedAt", LocalDateTime.now().minusMinutes(5).toString());
                common.put("trackingId", "FRM-992100");
                common.put("portalUrl", "https://app.anastasia.com/portal/forms/FRM-992100");
            }
            case FORM_APPROVED -> {
                common.put("formName", "Baptism Request");
                common.put("approvedAt", LocalDateTime.now().toString());
                common.put("portalUrl", "https://app.anastasia.com/portal/forms/FRM-992100");
            }
            case ADMIN_DIGEST -> {
                common.put("newMembersCount", 12);
                common.put("pendingFormsCount", 7);
                common.put("upcomingEventsCount", 5);
                common.put("dashboardUrl", "https://app.anastasia.com/admin/dashboard");
            }
        }

        return common;
    }
}

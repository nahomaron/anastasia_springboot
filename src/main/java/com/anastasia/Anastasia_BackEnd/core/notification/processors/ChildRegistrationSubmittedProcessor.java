package com.anastasia.Anastasia_BackEnd.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import org.springframework.stereotype.Component;

@Component
public class ChildRegistrationSubmittedProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;

    public ChildRegistrationSubmittedProcessor(EmailNotificationService emailService,
                                               InAppNotificationService inAppService) {
        this.emailService = emailService;
        this.inAppService = inAppService;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.CHILD_REGISTRATION_SUBMITTED;
    }

    @Override
    public void process(NotificationEvent event) {
        emailService.sendEmail(event, "New Child Registration Submitted", EmailTemplateName.NOTIFICATION);
        inAppService.send(event);
    }
}

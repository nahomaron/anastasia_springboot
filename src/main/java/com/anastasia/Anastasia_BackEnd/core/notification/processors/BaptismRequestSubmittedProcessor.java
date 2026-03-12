package com.anastasia.Anastasia_BackEnd.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import org.springframework.stereotype.Component;

@Component
public class BaptismRequestSubmittedProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;

    public BaptismRequestSubmittedProcessor(EmailNotificationService emailService,
                                           InAppNotificationService inAppService) {
        this.emailService = emailService;
        this.inAppService = inAppService;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.BAPTISM_REQUEST_SUBMITTED;
    }

    @Override
    public void process(NotificationEvent event) {
        emailService.sendEmail(event, "New Baptism Request Submitted", EmailTemplateName.NOTIFICATION);
        inAppService.send(event);
    }
}

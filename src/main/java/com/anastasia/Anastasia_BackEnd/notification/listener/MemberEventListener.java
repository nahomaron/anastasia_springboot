package com.anastasia.Anastasia_BackEnd.notification.listener;

import com.anastasia.Anastasia_BackEnd.notification.domain.events.MemberBirthdayEvent;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationChannelType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MemberEventListener {

    private final ApplicationEventPublisher publisher;

    public MemberEventListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onMemberBirthday(MemberBirthdayEvent event) {
        MemberEntity member = event.getMember();

        Map<String, Object> props = new java.util.HashMap<>();
        props.put("memberName", member.getFirstName() + " " + member.getFatherName());
        props.put("birthdayMessage", "Happy Birthday " + member.getFirstName() + " 🎉");
        if (member.getPhone() != null) {
            props.put("phone", member.getPhone());
        }
        if (member.getWhatsApp() != null) {
            props.put("whatsApp", member.getWhatsApp());
        }

        publisher.publishEvent(
                new NotificationEvent(
                        this,
                        NotificationType.MEMBER_BIRTHDAY,
                        member.getUser(),
                        props,
                        java.util.EnumSet.of(
                                NotificationChannelType.EMAIL,
                                NotificationChannelType.SMS,
                                NotificationChannelType.WHATSAPP,
                                NotificationChannelType.IN_APP)
                )
        );
    }
}

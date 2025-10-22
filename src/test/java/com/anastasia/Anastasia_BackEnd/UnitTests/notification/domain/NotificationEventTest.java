package com.anastasia.Anastasia_BackEnd.UnitTests.notification.domain;

import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventTest {

    @Test
    void defaultsToEmailAndInAppChannels() {
        NotificationEvent event = new NotificationEvent(this, NotificationType.WELCOME, null, Map.of());

        assertThat(event.getChannels()).containsExactlyInAnyOrder(
                NotificationChannelType.EMAIL,
                NotificationChannelType.IN_APP
        );
    }

    @Test
    void honoursExplicitChannelSelection() {
        NotificationEvent event = new NotificationEvent(
                this,
                NotificationType.NOTIFICATION,
                null,
                Map.of(),
                EnumSet.of(NotificationChannelType.SMS)
        );

        assertThat(event.getChannels()).containsExactly(NotificationChannelType.SMS);
    }

    @Test
    void buildsTargetUsingMembershipDetails() {
        MemberEntity member = new MemberEntity();
        member.setPhone("+1555123456");
        member.setWhatsApp("+1555987654");

        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setTenantId(UUID.randomUUID());
        user.assignMembership(member);

        NotificationEvent event = new NotificationEvent(this, NotificationType.NOTIFICATION, user, Map.of());

        assertThat(event.getTarget().phoneNumber()).isEqualTo("+1555123456");
        assertThat(event.getTarget().whatsAppNumber()).isEqualTo("+1555987654");
        assertThat(event.getTarget().email()).isEqualTo("user@example.com");
        assertThat(event.getTarget().tenantId()).isEqualTo(user.getTenantId());
    }
}

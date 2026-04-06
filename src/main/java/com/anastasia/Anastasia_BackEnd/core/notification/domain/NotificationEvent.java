package com.anastasia.Anastasia_BackEnd.core.notification.domain;


import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public class NotificationEvent {
    private final Object source;
    private final NotificationType type;
    private final UserEntity user;
    private final Map<String, Object> properties;
    private final NotificationTarget target;
    private final Set<NotificationChannelType> channels;

    public NotificationEvent(Object source, NotificationType type, UserEntity user, Map<String, Object> properties) {
        this(source, type, user, properties, null);
    }

    public NotificationEvent(Object source,
                             NotificationType type,
                             UserEntity user,
                             Map<String, Object> properties,
                             Set<NotificationChannelType> channels) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.user = user;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        Set<NotificationChannelType> resolvedChannels =
                (channels == null || channels.isEmpty())
                        ? EnumSet.of(NotificationChannelType.EMAIL, NotificationChannelType.IN_APP)
                        : EnumSet.copyOf(channels);
        this.channels = Set.copyOf(resolvedChannels);
        this.target = NotificationTarget.fromUser(user, this.properties);
    }

    public boolean requiresChannel(NotificationChannelType channelType) {
        return channels.contains(channelType);
    }
}

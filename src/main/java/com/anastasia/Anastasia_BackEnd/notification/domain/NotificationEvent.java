package com.anastasia.Anastasia_BackEnd.notification.domain;


import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public class NotificationEvent extends ApplicationEvent {
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
        super(source);
        this.type = Objects.requireNonNull(type, "type");
        this.user = user;
        this.properties = properties == null ? Map.of() : Collections.unmodifiableMap(properties);
        Set<NotificationChannelType> resolvedChannels =
                (channels == null || channels.isEmpty())
                        ? EnumSet.of(NotificationChannelType.EMAIL, NotificationChannelType.IN_APP)
                        : EnumSet.copyOf(channels);
        this.channels = Collections.unmodifiableSet(resolvedChannels);
        this.target = NotificationTarget.fromUser(user, this.properties);
    }

    public boolean requiresChannel(NotificationChannelType channelType) {
        return channels.contains(channelType);
    }
}

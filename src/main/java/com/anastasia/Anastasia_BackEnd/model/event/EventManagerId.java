package com.anastasia.Anastasia_BackEnd.model.event;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class EventManagerId implements Serializable {
    private Long eventId;
    private UUID userId;

    // Required: equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventManagerId)) return false;
        EventManagerId that = (EventManagerId) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, userId);
    }
}

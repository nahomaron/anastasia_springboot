package com.anastasia.Anastasia_BackEnd.common.utils;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AttendanceTimeValidator {

    @Value("${attendance.grace.before:15}")
    private int graceBeforeMinutes;

    @Value("${attendance.grace.after:15}")
    private int graceAfterMinutes;

    private Duration graceBefore;
    private Duration graceAfter;

    @PostConstruct
    public void init() {
        this.graceBefore = Duration.ofMinutes(graceBeforeMinutes);
        this.graceAfter = Duration.ofMinutes(graceAfterMinutes);
    }

    public boolean isCheckInAllowed(EventEntity event) {
        Instant startAt = event.getStartAt();
        Instant endAt = event.getEndAt();
        if (startAt == null || endAt == null) {
            return false;
        }

        Instant now = Instant.now();
        Instant allowedStart = startAt.minus(graceBefore);
        Instant allowedEnd = endAt.plus(graceAfter);

        return !now.isBefore(allowedStart) && !now.isAfter(allowedEnd);
    }
}

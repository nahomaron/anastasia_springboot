package com.anastasia.Anastasia_BackEnd.common.utils;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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

        ZoneId zoneId = resolveZone(event.getTimezone());
        ZonedDateTime startAtLocal = startAt.atZone(zoneId);
        ZonedDateTime endAtLocal = endAt.atZone(zoneId);
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);

        if (!today.isEqual(startAtLocal.toLocalDate())) return false;

        LocalTime allowedStart = startAtLocal.toLocalTime().minus(graceBefore);
        LocalTime allowedEnd = endAtLocal.toLocalTime().plus(graceAfter);

        return !now.isBefore(allowedStart) && !now.isAfter(allowedEnd);
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(timezone);
    }
}

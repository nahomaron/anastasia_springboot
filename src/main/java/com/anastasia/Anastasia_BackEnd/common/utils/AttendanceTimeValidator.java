package com.anastasia.Anastasia_BackEnd.common.utils;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        LocalDateTime startAt = event.getStartAt();
        LocalDateTime endAt = event.getEndAt();
        if (startAt == null || endAt == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (!today.isEqual(startAt.toLocalDate())) return false;

        LocalTime allowedStart = startAt.toLocalTime().minus(graceBefore);
        LocalTime allowedEnd = endAt.toLocalTime().plus(graceAfter);

        return !now.isBefore(allowedStart) && !now.isAfter(allowedEnd);
    }
}

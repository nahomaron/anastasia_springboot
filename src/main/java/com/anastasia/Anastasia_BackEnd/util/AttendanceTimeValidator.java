package com.anastasia.Anastasia_BackEnd.util;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
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
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (!today.isEqual(event.getDate())) return false;

        LocalTime allowedStart = event.getStartTime().minus(graceBefore);
        LocalTime allowedEnd = event.getEndTime().plus(graceAfter);

        return !now.isBefore(allowedStart) && !now.isAfter(allowedEnd);
    }
}

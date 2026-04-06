package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.common.utils.AttendanceTimeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceTimeValidatorTest {

    private AttendanceTimeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AttendanceTimeValidator();
        ReflectionTestUtils.setField(validator, "graceBeforeMinutes", 15);
        ReflectionTestUtils.setField(validator, "graceAfterMinutes", 10);
        validator.init();
    }

    @Test
    void isCheckInAllowed_whenWithinWindow_shouldReturnTrue() {
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();
        ZonedDateTime startAt = now.atZone(zone).plusMinutes(10);
        ZonedDateTime endAt = startAt.plusMinutes(20);
        EventEntity event = EventEntity.builder()
                .startAt(startAt.toInstant())
                .endAt(endAt.toInstant())
                .timezone(zone.getId())
                .build();

        assertThat(validator.isCheckInAllowed(event)).isTrue();
    }

    @Test
    void isCheckInAllowed_whenDifferentDay_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .startAt(LocalDate.now().plusDays(1).atTime(now).toInstant(ZoneOffset.UTC))
                .endAt(LocalDate.now().plusDays(1).atTime(now.plusMinutes(30)).toInstant(ZoneOffset.UTC))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }

    @Test
    void isCheckInAllowed_whenOutsideWindow_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .startAt(LocalDate.now().atTime(now.plusHours(2)).toInstant(ZoneOffset.UTC))
                .endAt(LocalDate.now().atTime(now.plusHours(3)).toInstant(ZoneOffset.UTC))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }
}

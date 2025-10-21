package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.util.AttendanceTimeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

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
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .date(LocalDate.now())
                .startTime(now.plusMinutes(10))
                .endTime(now.plusMinutes(30))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isTrue();
    }

    @Test
    void isCheckInAllowed_whenDifferentDay_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .date(LocalDate.now().plusDays(1))
                .startTime(now)
                .endTime(now.plusMinutes(30))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }

    @Test
    void isCheckInAllowed_whenOutsideWindow_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .date(LocalDate.now())
                .startTime(now.plusHours(2))
                .endTime(now.plusHours(3))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }
}

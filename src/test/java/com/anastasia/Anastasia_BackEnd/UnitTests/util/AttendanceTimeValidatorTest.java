package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.common.utils.AttendanceTimeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
                .startAt(LocalDateTime.now().with(now.plusMinutes(10)))
                .endAt(LocalDateTime.now().with(now.plusMinutes(30)))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isTrue();
    }

    @Test
    void isCheckInAllowed_whenDifferentDay_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .startAt(LocalDateTime.now().plusDays(1).with(now))
                .endAt(LocalDateTime.now().plusDays(1).with(now.plusMinutes(30)))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }

    @Test
    void isCheckInAllowed_whenOutsideWindow_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        EventEntity event = EventEntity.builder()
                .startAt(LocalDateTime.now().with(now.plusHours(2)))
                .endAt(LocalDateTime.now().with(now.plusHours(3)))
                .build();

        assertThat(validator.isCheckInAllowed(event)).isFalse();
    }
}

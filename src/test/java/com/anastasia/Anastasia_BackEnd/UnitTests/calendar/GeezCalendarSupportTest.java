package com.anastasia.Anastasia_BackEnd.UnitTests.calendar;

import com.anastasia.Anastasia_BackEnd.modules.calendar.service.GeezCalendarSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GeezCalendarSupportTest {

    private final GeezCalendarSupport support = new GeezCalendarSupport();

    @Test
    void roundTripsMeskeremBoundaryDate() {
        LocalDate original = LocalDate.of(2025, 9, 11);
        GeezCalendarSupport.GeezDate geezDate = support.toGeezFromGregorian(original);

        assertThat(geezDate.month()).isEqualTo(1);
        assertThat(geezDate.day()).isEqualTo(1);
        assertThat(support.toGregorianFromGeez(geezDate.year(), geezDate.month(), geezDate.day()))
                .isEqualTo(original);
    }

    @Test
    void roundTripsChristmasBoundaryDate() {
        LocalDate original = LocalDate.of(2026, 1, 7);
        GeezCalendarSupport.GeezDate geezDate = support.toGeezFromGregorian(original);

        assertThat(geezDate.month()).isEqualTo(4);
        assertThat(geezDate.day()).isEqualTo(29);
        assertThat(support.toGregorianFromGeez(geezDate.year(), geezDate.month(), geezDate.day()))
                .isEqualTo(original);
    }
}

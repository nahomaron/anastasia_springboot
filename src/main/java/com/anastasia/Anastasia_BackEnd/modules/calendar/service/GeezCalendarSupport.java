package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class GeezCalendarSupport {

    private static final int JD_OFFSET_GREGORIAN = 1721425;
    private static final int JD_OFFSET_GEEZ = 1723856;

    public GeezDate toGeezFromGregorian(LocalDate date) {
        int jdn = gregorianToJdn(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        return jdnToGeez(jdn);
    }

    public LocalDate toGregorianFromGeez(int year, int month, int day) {
        int jdn = geezToJdn(year, month, day);
        GregorianDate gregorian = jdnToGregorian(jdn);
        return LocalDate.of(gregorian.year(), gregorian.month(), gregorian.day());
    }

    private int geezToJdn(int year, int month, int day) {
        return JD_OFFSET_GEEZ
                + 365 * (year - 1)
                + Math.floorDiv(year, 4)
                + 30 * (month - 1)
                + (day - 1);
    }

    private GeezDate jdnToGeez(int jdn) {
        int r = Math.floorMod(jdn - JD_OFFSET_GEEZ, 1461);
        int n = Math.floorMod(r, 365) + 365 * Math.floorDiv(r, 1460);
        int year = 4 * Math.floorDiv(jdn - JD_OFFSET_GEEZ, 1461)
                + Math.floorDiv(r, 365)
                - Math.floorDiv(r, 1460)
                + 1;
        int month = Math.floorDiv(n, 30) + 1;
        int day = Math.floorMod(n, 30) + 1;
        return new GeezDate(year, month, day);
    }

    private int gregorianToJdn(int year, int month, int day) {
        int a = Math.floorDiv(14 - month, 12);
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day
                + Math.floorDiv(153 * m + 2, 5)
                + 365 * y
                + Math.floorDiv(y, 4)
                - Math.floorDiv(y, 100)
                + Math.floorDiv(y, 400)
                - 32045;
    }

    private GregorianDate jdnToGregorian(int jdn) {
        int f = jdn + 1401 + (((4 * jdn + 274277) / 146097) * 3) / 4 - 38;
        int e = 4 * f + 3;
        int g = Math.floorMod(e, 1461) / 4;
        int h = 5 * g + 2;
        int day = Math.floorMod(h, 153) / 5 + 1;
        int month = Math.floorMod(h / 153 + 2, 12) + 1;
        int year = e / 1461 - 4716 + (12 + 2 - month) / 12;
        return new GregorianDate(year, month, day);
    }

    public record GeezDate(int year, int month, int day) {
    }

    private record GregorianDate(int year, int month, int day) {
    }
}

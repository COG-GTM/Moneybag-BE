package com.babakjan.moneybag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AgeCalculationServiceTest {

    private AgeCalculationService ageCalculationService;

    @BeforeEach
    void setUp() {
        ageCalculationService = new AgeCalculationService();
    }

    private Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day); // month is 0-indexed
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    @DisplayName("Calculate age in calendar year correctly")
    void calculateAgeInCalendarYear() {
        Date dob = createDate(1974, 5, 15);
        assertEquals(52, ageCalculationService.calculateAgeInCalendarYear(dob, 2026));
    }

    @Test
    @DisplayName("Age 52 is catch-up eligible")
    void isCatchUpEligible_age52() {
        Date dob = createDate(1974, 5, 15);
        assertTrue(ageCalculationService.isCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Age 48 is not catch-up eligible")
    void isNotCatchUpEligible_age48() {
        Date dob = createDate(1978, 5, 15);
        assertFalse(ageCalculationService.isCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Age 61 is super catch-up eligible")
    void isSuperCatchUpEligible_age61() {
        Date dob = createDate(1965, 3, 10);
        assertTrue(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Age 60 is super catch-up eligible")
    void isSuperCatchUpEligible_age60() {
        Date dob = createDate(1966, 10, 15); // Turns 60 in Oct 2026
        assertTrue(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Age 63 is super catch-up eligible")
    void isSuperCatchUpEligible_age63() {
        Date dob = createDate(1963, 6, 1);
        assertTrue(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Age 64 reverts to standard catch-up, not super")
    void isNotSuperCatchUpEligible_age64() {
        Date dob = createDate(1962, 3, 15); // Turns 64 in Mar 2026
        assertFalse(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
        assertTrue(ageCalculationService.isCatchUpEligible(dob, 2026));
        assertTrue(ageCalculationService.isStandardCatchUpOnly(dob, 2026));
    }

    @Test
    @DisplayName("Age 59 is standard catch-up only, not super")
    void isStandardCatchUpOnly_age59() {
        Date dob = createDate(1967, 7, 20);
        assertTrue(ageCalculationService.isCatchUpEligible(dob, 2026));
        assertFalse(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
        assertTrue(ageCalculationService.isStandardCatchUpOnly(dob, 2026));
    }

    @Test
    @DisplayName("Edge case: Turns 60 in October 2026 — eligible for super catch-up for full 2026")
    void edgeCase_turns60InOct2026() {
        Date dob = createDate(1966, 10, 20);
        assertEquals(60, ageCalculationService.calculateAgeInCalendarYear(dob, 2026));
        assertTrue(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
    }

    @Test
    @DisplayName("Edge case: Turns 64 in March 2026 — reverts to standard catch-up for full 2026")
    void edgeCase_turns64InMar2026() {
        Date dob = createDate(1962, 3, 10);
        assertEquals(64, ageCalculationService.calculateAgeInCalendarYear(dob, 2026));
        assertFalse(ageCalculationService.isSuperCatchUpEligible(dob, 2026));
        assertTrue(ageCalculationService.isStandardCatchUpOnly(dob, 2026));
    }
}

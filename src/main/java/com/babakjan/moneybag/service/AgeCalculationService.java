package com.babakjan.moneybag.service;

import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class AgeCalculationService {

    /**
     * Calculate the age a person attains during a given calendar year.
     * Per IRS rules, age is determined by the age attained during the calendar year,
     * not at the time of contribution.
     *
     * @param dateOfBirth the participant's date of birth
     * @param calendarYear the calendar year to check
     * @return age attained during the calendar year
     */
    public int calculateAgeInCalendarYear(Date dateOfBirth, int calendarYear) {
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(dateOfBirth);
        int birthYear = birthCal.get(Calendar.YEAR);
        return calendarYear - birthYear;
    }

    /**
     * Determine if participant is eligible for catch-up contributions (age 50+).
     *
     * @param dateOfBirth the participant's date of birth
     * @param calendarYear the calendar year to check
     * @return true if age >= 50 in the calendar year
     */
    public boolean isCatchUpEligible(Date dateOfBirth, int calendarYear) {
        return calculateAgeInCalendarYear(dateOfBirth, calendarYear) >= 50;
    }

    /**
     * Determine if participant is eligible for super catch-up contributions (ages 60-63).
     *
     * @param dateOfBirth the participant's date of birth
     * @param calendarYear the calendar year to check
     * @return true if age is 60-63 inclusive in the calendar year
     */
    public boolean isSuperCatchUpEligible(Date dateOfBirth, int calendarYear) {
        int age = calculateAgeInCalendarYear(dateOfBirth, calendarYear);
        return age >= 60 && age <= 63;
    }

    /**
     * Determine if participant is eligible for standard catch-up only (age 50+ but not 60-63).
     *
     * @param dateOfBirth the participant's date of birth
     * @param calendarYear the calendar year to check
     * @return true if age is 50+ but not in the 60-63 super catch-up range
     */
    public boolean isStandardCatchUpOnly(Date dateOfBirth, int calendarYear) {
        int age = calculateAgeInCalendarYear(dateOfBirth, calendarYear);
        return age >= 50 && !(age >= 60 && age <= 63);
    }
}

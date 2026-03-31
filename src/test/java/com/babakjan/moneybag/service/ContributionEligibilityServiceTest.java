package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.contribution.ContributionEligibilityResponse;
import com.babakjan.moneybag.entity.*;
import com.babakjan.moneybag.error.exception.ContributionRoutingException;
import com.babakjan.moneybag.repository.ContributionLimitConfigRepository;
import com.babakjan.moneybag.repository.ParticipantProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionEligibilityServiceTest {

    @Mock
    private ParticipantProfileRepository participantProfileRepository;

    @Mock
    private ContributionLimitConfigRepository contributionLimitConfigRepository;

    @Spy
    private AgeCalculationService ageCalculationService;

    @InjectMocks
    private ContributionEligibilityService eligibilityService;

    private ContributionLimitConfig config2026;

    private Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @BeforeEach
    void setUp() {
        config2026 = ContributionLimitConfig.builder()
                .id(1L)
                .planYear(2026)
                .ficaWageThreshold(150000.0)
                .standardCatchUpLimit(8000.0)
                .superCatchUpLimit(11250.0)
                .regularContributionLimit(23500.0)
                .effectiveDate(createDate(2026, 1, 1))
                .build();
    }

    private ParticipantProfile buildProfile(Date dob, Double ficaWages, Double controlledGroupWages,
                                            Boolean isNewHire, PlanType planType,
                                            Boolean specialCatchUpEligible, Double specialCatchUpLimit) {
        return ParticipantProfile.builder()
                .id(1L)
                .userId(100L)
                .dateOfBirth(dob)
                .priorYearFicaWages(ficaWages)
                .ficaWagesFromControlledGroup(controlledGroupWages)
                .isNewHire(isNewHire)
                .employerPlanType(planType)
                .specialCatchUpEligible(specialCatchUpEligible)
                .specialCatchUpLimit(specialCatchUpLimit)
                .build();
    }

    @Test
    @DisplayName("Test #1: Age 52, FICA $160K — high earner, catch-up eligible, Roth required")
    void test1_age52_fica160k_highEarner() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        assertFalse(response.isSuperCatchUpEligible());
        assertEquals(8000.0, response.getCatchUpLimit());
        assertEquals(ContributionDesignation.ROTH, response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #3: Age 52, FICA $130K — not high earner, no required designation")
    void test3_age52_fica130k_notHighEarner() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 130000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertFalse(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        assertNull(response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #5: Age 48 — not catch-up eligible")
    void test5_age48_notCatchUpEligible() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1978, 5, 15), 200000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isHighEarner());
        assertFalse(response.isCatchUpEligible());
        assertNull(response.getCatchUpLimit());
    }

    @Test
    @DisplayName("Test #6: FICA exactly $150K (at threshold) — NOT high earner ('in excess of')")
    void test6_ficaExactlyAtThreshold_notHighEarner() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 150000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertFalse(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        assertNull(response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #7: Age 61, FICA $160K — super catch-up eligible, Roth required")
    void test7_age61_highEarner_superCatchUp() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1965, 3, 10), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        assertTrue(response.isSuperCatchUpEligible());
        assertEquals(11250.0, response.getCatchUpLimit());
        assertEquals(ContributionDesignation.ROTH, response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #8: Age 61, FICA $130K — super catch-up eligible, no Roth required")
    void test8_age61_notHighEarner_superCatchUp() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1965, 3, 10), 130000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertFalse(response.isHighEarner());
        assertTrue(response.isSuperCatchUpEligible());
        assertEquals(11250.0, response.getCatchUpLimit());
        assertNull(response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #9: Age 64 — reverts to standard catch-up limit")
    void test9_age64_revertsToStandardCatchUp() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1962, 3, 15), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isCatchUpEligible());
        assertFalse(response.isSuperCatchUpEligible());
        assertEquals(8000.0, response.getCatchUpLimit());
    }

    @Test
    @DisplayName("Test #11: Turns 60 in Oct 2026 — eligible for super catch-up for full 2026")
    void test11_turns60InOct2026_superCatchUpFullYear() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1966, 10, 20), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isSuperCatchUpEligible());
        assertEquals(11250.0, response.getCatchUpLimit());
    }

    @Test
    @DisplayName("Test #12: Turns 64 in Mar 2026 — reverts to standard catch-up for full 2026")
    void test12_turns64InMar2026_revertsToStandardFullYear() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1962, 3, 10), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertFalse(response.isSuperCatchUpEligible());
        assertEquals(8000.0, response.getCatchUpLimit());
    }

    @Test
    @DisplayName("Test #13: New hire, no prior-year W-2 — allow pre-tax catch-up (good-faith)")
    void test13_newHire_noPriorYearW2_allowPreTax() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), null, null, true, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertFalse(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        assertNull(response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Test #14: FICA from controlled group: $80K + $75K = $155K — high earner (aggregated > $150K)")
    void test14_controlledGroup_aggregated_highEarner() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 80000.0, 75000.0, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isHighEarner());
        assertEquals(ContributionDesignation.ROTH, response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Governmental 457(b) special catch-up — exempt from Roth requirement")
    void governmental457b_specialCatchUp_exemptFromRoth() throws ContributionRoutingException {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 160000.0, null, false,
                PlanType.GOVERNMENTAL_457B, true, 20000.0);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2026)).thenReturn(Optional.of(config2026));

        ContributionEligibilityResponse response = eligibilityService.determineEligibility(1L, 2026);

        assertTrue(response.isHighEarner());
        assertTrue(response.isCatchUpEligible());
        // Special catch-up amounts are exempt from Roth requirement
        assertNull(response.getRequiredDesignation());
    }

    @Test
    @DisplayName("Participant not found — throws ContributionRoutingException")
    void participantNotFound_throwsException() {
        when(participantProfileRepository.findById(999L)).thenReturn(Optional.empty());

        ContributionRoutingException exception = assertThrows(
                ContributionRoutingException.class,
                () -> eligibilityService.determineEligibility(999L, 2026));
        assertEquals("PARTICIPANT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    @DisplayName("Config not found — throws ContributionRoutingException")
    void configNotFound_throwsException() {
        ParticipantProfile profile = buildProfile(
                createDate(1974, 5, 15), 160000.0, null, false, PlanType.PLAN_401K, false, null);

        when(participantProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(contributionLimitConfigRepository.findByPlanYear(2030)).thenReturn(Optional.empty());

        ContributionRoutingException exception = assertThrows(
                ContributionRoutingException.class,
                () -> eligibilityService.determineEligibility(1L, 2030));
        assertEquals("CONFIG_NOT_FOUND", exception.getErrorCode());
    }
}

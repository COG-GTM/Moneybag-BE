package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.contribution.ContributionEligibilityResponse;
import com.babakjan.moneybag.dto.contribution.ContributionRequest;
import com.babakjan.moneybag.dto.contribution.ContributionResponse;
import com.babakjan.moneybag.entity.ContributionDesignation;
import com.babakjan.moneybag.entity.ContributionType;
import com.babakjan.moneybag.error.exception.ContributionRoutingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionRoutingEngineTest {

    @Mock
    private ContributionEligibilityService eligibilityService;

    @InjectMocks
    private ContributionRoutingEngine routingEngine;

    private static final int PLAN_YEAR = 2026;

    private ContributionEligibilityResponse buildEligibility(
            boolean highEarner, boolean catchUpEligible, boolean superCatchUpEligible,
            Double catchUpLimit, ContributionDesignation requiredDesignation) {
        return ContributionEligibilityResponse.builder()
                .participantId(1L)
                .planYear(PLAN_YEAR)
                .isHighEarner(highEarner)
                .isCatchUpEligible(catchUpEligible)
                .isSuperCatchUpEligible(superCatchUpEligible)
                .catchUpLimit(catchUpLimit)
                .requiredDesignation(requiredDesignation)
                .build();
    }

    // ==========================================
    // Core Routing Tests (#1-#6)
    // ==========================================

    @Test
    @DisplayName("Test #1: Age 52, FICA $160K, pre-tax catch-up — Rejected ROTH_CATCHUP_REQUIRED")
    void test1_age52_highEarner_preTaxCatchUp_rejected() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, true, false, 8000.0, ContributionDesignation.ROTH));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertFalse(response.isAccepted());
        assertEquals("ROTH_CATCHUP_REQUIRED", response.getErrorCode());
    }

    @Test
    @DisplayName("Test #2: Age 52, FICA $160K, Roth catch-up — Accepted")
    void test2_age52_highEarner_rothCatchUp_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, true, false, 8000.0, ContributionDesignation.ROTH));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.ROTH)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #3: Age 52, FICA $130K, pre-tax catch-up — Accepted (not high earner)")
    void test3_age52_notHighEarner_preTaxCatchUp_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, false, 8000.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #4: Age 52, FICA $130K, Roth catch-up — Accepted")
    void test4_age52_notHighEarner_rothCatchUp_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, false, 8000.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.ROTH)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #5: Age 48, FICA $200K, regular contribution — Accepted (Section 603 doesn't apply)")
    void test5_age48_regularContribution_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, false, false, null, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(23500.0)
                .contributionType(ContributionType.REGULAR)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #6: Age 52, FICA exactly $145K (at threshold), pre-tax catch-up — Accepted ('in excess of')")
    void test6_age52_exactlyAtThreshold_preTaxCatchUp_accepted() throws ContributionRoutingException {
        // Exactly at threshold means NOT affected — "in excess of" means strictly greater than
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, false, 8000.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    // ==========================================
    // Super Catch-Up Tests (#7-#10)
    // ==========================================

    @Test
    @DisplayName("Test #7: Age 61, FICA $160K, $11,250 super catch-up Roth — Accepted")
    void test7_age61_highEarner_superCatchUp_roth_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, true, true, 11250.0, ContributionDesignation.ROTH));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(11250.0)
                .contributionType(ContributionType.SUPER_CATCHUP)
                .designation(ContributionDesignation.ROTH)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #8: Age 61, FICA $130K, $11,250 super catch-up pre-tax — Accepted (not high earner)")
    void test8_age61_notHighEarner_superCatchUp_preTax_accepted() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, true, 11250.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(11250.0)
                .contributionType(ContributionType.SUPER_CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #9: Age 64, FICA $160K, $8K standard catch-up Roth — Accepted (reverts to standard)")
    void test9_age64_highEarner_standardCatchUp_roth_accepted() throws ContributionRoutingException {
        // Age 64 reverts to standard catch-up (not super)
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, true, false, 8000.0, ContributionDesignation.ROTH));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.ROTH)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    @Test
    @DisplayName("Test #10: Age 59, FICA $160K, $8K standard catch-up Roth — Accepted (no super)")
    void test10_age59_highEarner_standardCatchUp_roth_accepted() throws ContributionRoutingException {
        // Age 59 is standard catch-up only, not super
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(true, true, false, 8000.0, ContributionDesignation.ROTH));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.ROTH)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertTrue(response.isAccepted());
    }

    // ==========================================
    // Additional Edge Cases
    // ==========================================

    @Test
    @DisplayName("Under-50 participant trying catch-up — Rejected CATCHUP_NOT_ELIGIBLE")
    void under50_catchUp_rejected() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, false, false, null, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(8000.0)
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertFalse(response.isAccepted());
        assertEquals("CATCHUP_NOT_ELIGIBLE", response.getErrorCode());
    }

    @Test
    @DisplayName("Non-super-eligible participant trying super catch-up — Rejected")
    void nonSuperEligible_superCatchUp_rejected() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, false, 8000.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(11250.0)
                .contributionType(ContributionType.SUPER_CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertFalse(response.isAccepted());
        assertEquals("CATCHUP_NOT_ELIGIBLE", response.getErrorCode());
    }

    @Test
    @DisplayName("Contribution amount exceeds applicable limit — Rejected")
    void contributionExceedsLimit_rejected() throws ContributionRoutingException {
        when(eligibilityService.determineEligibility(anyLong(), anyInt()))
                .thenReturn(buildEligibility(false, true, false, 8000.0, null));

        ContributionRequest request = ContributionRequest.builder()
                .participantId(1L)
                .amount(9000.0) // Exceeds $8,000 standard limit
                .contributionType(ContributionType.CATCHUP)
                .designation(ContributionDesignation.PRE_TAX)
                .planYear(PLAN_YEAR)
                .build();

        ContributionResponse response = routingEngine.routeContribution(request);
        assertFalse(response.isAccepted());
        assertEquals("CONTRIBUTION_LIMIT_EXCEEDED", response.getErrorCode());
    }
}

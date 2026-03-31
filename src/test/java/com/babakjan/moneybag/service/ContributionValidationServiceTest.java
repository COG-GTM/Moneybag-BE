package com.babakjan.moneybag.service;

import com.babakjan.moneybag.entity.*;
import com.babakjan.moneybag.error.exception.ContributionValidationException;
import com.babakjan.moneybag.repository.ContributionLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContributionValidationServiceTest {

    @InjectMocks
    private ContributionValidationService validationService;

    @Mock
    private ContributionLimitRepository contributionLimitRepository;

    private ContributionLimit limit2026;
    private RetirementPlan plan;

    @BeforeEach
    void setUp() {
        limit2026 = ContributionLimit.builder()
                .id(1L)
                .taxYear(2026)
                .regularLimit(23500.0)
                .catchUpLimit(7500.0)
                .superCatchUpLimit(11250.0)
                .ficaWagesThreshold(145000.0)
                .build();

        plan = RetirementPlan.builder()
                .id(1L)
                .planType(PlanType.PLAN_401K)
                .planName("Company 401k")
                .employerName("Acme Corp")
                .build();
    }

    private Participant buildParticipant(int ageAtEndOf2026, double ficaWages) {
        // Calculate date of birth so that age as of Dec 31 2026 is the desired age
        LocalDate dob = LocalDate.of(2026 - ageAtEndOf2026, 6, 15);
        return Participant.builder()
                .id(1L)
                .dateOfBirth(dob)
                .priorYearFicaWages(ficaWages)
                .build();
    }

    private Participant buildParticipantWithDob(LocalDate dob, double ficaWages) {
        return Participant.builder()
                .id(1L)
                .dateOfBirth(dob)
                .priorYearFicaWages(ficaWages)
                .build();
    }

    private Contribution buildContribution(Participant participant, ContributionType type, TaxTreatment treatment) {
        return Contribution.builder()
                .participant(participant)
                .plan(plan)
                .amount(5000.0)
                .contributionType(type)
                .taxTreatment(treatment)
                .payrollDate(LocalDate.of(2026, 6, 15))
                .build();
    }

    // Test 1: Age 45, $160k FICA, REGULAR, PRE_TAX — Accepted (no catch-up rules apply)
    @Test
    void test1_regularContribution_age45_highEarner_accepted() {
        Participant participant = buildParticipant(45, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.REGULAR, TaxTreatment.PRE_TAX);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
    }

    // Test 2: Age 52, $160k FICA, CATCH_UP, PRE_TAX — Rejected (must be Roth)
    @Test
    void test2_catchUp_age52_highEarner_preTax_rejected() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(52, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertThatThrownBy(() -> validationService.validateContribution(contribution))
                .isInstanceOf(ContributionValidationException.class)
                .hasMessageContaining("Roth");
    }

    // Test 3: Age 52, $160k FICA, CATCH_UP, ROTH — Accepted, limit $7,500
    @Test
    void test3_catchUp_age52_highEarner_roth_accepted() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(52, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(7500.0);
    }

    // Test 4: Age 52, $130k FICA, CATCH_UP, PRE_TAX — Accepted (below threshold)
    @Test
    void test4_catchUp_age52_belowThreshold_preTax_accepted() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(52, 130000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
    }

    // Test 5: Age 52, $130k FICA, CATCH_UP, ROTH — Accepted
    @Test
    void test5_catchUp_age52_belowThreshold_roth_accepted() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(52, 130000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
    }

    // Test 6: Age 61, $160k FICA, CATCH_UP, PRE_TAX — Rejected (must be Roth)
    @Test
    void test6_catchUp_age61_highEarner_preTax_rejected() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(61, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertThatThrownBy(() -> validationService.validateContribution(contribution))
                .isInstanceOf(ContributionValidationException.class)
                .hasMessageContaining("Roth");
    }

    // Test 7: Age 61, $160k FICA, CATCH_UP, ROTH — Accepted, super catch-up limit $11,250
    @Test
    void test7_catchUp_age61_highEarner_roth_accepted_superCatchUpLimit() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(61, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(11250.0);
    }

    // Test 8: Age 61, $130k FICA, CATCH_UP, PRE_TAX — Accepted, super catch-up limit $11,250
    @Test
    void test8_catchUp_age61_belowThreshold_preTax_accepted_superCatchUpLimit() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(61, 130000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(11250.0);
    }

    // Test 9: Age 61, $130k FICA, CATCH_UP, ROTH — Accepted, super catch-up limit $11,250
    @Test
    void test9_catchUp_age61_belowThreshold_roth_accepted_superCatchUpLimit() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(61, 130000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(11250.0);
    }

    // Test 10: Age 65, $160k FICA, CATCH_UP, PRE_TAX — Rejected (must be Roth, standard catch-up not super)
    @Test
    void test10_catchUp_age65_highEarner_preTax_rejected() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(65, 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertThatThrownBy(() -> validationService.validateContribution(contribution))
                .isInstanceOf(ContributionValidationException.class)
                .hasMessageContaining("Roth");
    }

    // Test 11: Age 65, $130k FICA, CATCH_UP, PRE_TAX — Accepted, limit $7,500
    @Test
    void test11_catchUp_age65_belowThreshold_preTax_accepted() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        Participant participant = buildParticipant(65, 130000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.PRE_TAX);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(7500.0);
    }

    // Test 12: Turns 60 mid-year, $160k FICA, CATCH_UP, ROTH — Accepted, super catch-up $11,250 (age as of Dec 31)
    @Test
    void test12_turns60MidYear_highEarner_roth_accepted_superCatchUp() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        // Born July 15, 1966 — turns 60 on July 15, 2026 (age 60 as of Dec 31, 2026)
        Participant participant = buildParticipantWithDob(LocalDate.of(1966, 7, 15), 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(11250.0);
    }

    // Test 13: Turns 64 mid-year, $160k FICA, CATCH_UP, ROTH — Accepted, standard catch-up $7,500 (age 64 by Dec 31, outside 60-63)
    @Test
    void test13_turns64MidYear_highEarner_roth_accepted_standardCatchUp() {
        given(contributionLimitRepository.findByTaxYear(2026))
                .willReturn(Optional.of(limit2026));

        // Born July 15, 1962 — turns 64 on July 15, 2026 (age 64 as of Dec 31, 2026)
        Participant participant = buildParticipantWithDob(LocalDate.of(1962, 7, 15), 160000.0);
        Contribution contribution = buildContribution(participant, ContributionType.CATCH_UP, TaxTreatment.ROTH);

        assertDoesNotThrow(() -> validationService.validateContribution(contribution));
        assertThat(validationService.getApplicableCatchUpLimit(participant, 2026)).isEqualTo(7500.0);
    }
}

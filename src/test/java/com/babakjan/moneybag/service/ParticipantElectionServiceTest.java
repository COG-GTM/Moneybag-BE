package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.participant.ElectionSubmissionRequest;
import com.babakjan.moneybag.dto.participant.ElectionSubmissionResponse;
import com.babakjan.moneybag.dto.participant.ParticipantElectionOptionsResponse;
import com.babakjan.moneybag.entity.ParticipantElection;
import com.babakjan.moneybag.error.exception.ElectionValidationException;
import com.babakjan.moneybag.repository.ParticipantElectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ParticipantElectionServiceTest {

    @InjectMocks
    private ParticipantElectionService participantElectionService;

    @Mock
    private ParticipantElectionRepository participantElectionRepository;

    // ========== Scenario 1: Affected participant (age 55, FICA $180K) ==========
    @Test
    void scenario1_affectedParticipant_age55_highEarner_getElectionOptions() {
        // Participant 1: DOB 1971-03-15, FICA $180K, 401K, has Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(1L, 2026);

        assertThat(response.getParticipantId()).isEqualTo(1L);
        assertThat(response.getPlanYear()).isEqualTo(2026);
        assertThat(response.getAge()).isEqualTo(55);
        assertThat(response.isAffectedBySection603()).isTrue();
        assertThat(response.isCatchUpEligible()).isTrue();
        assertThat(response.isSuperCatchUpEligible()).isFalse();
        assertThat(response.isPreTaxCatchUpAvailable()).isFalse();
        assertThat(response.isRothCatchUpAvailable()).isTrue();
        assertThat(response.getCatchUpLimit()).isEqualTo(8000.0);
        assertThat(response.getSection603Message()).isNotNull();
        assertThat(response.getSection603Message()).contains("SECURE 2.0 Act");
        assertThat(response.getPlanNoRothMessage()).isNull();
    }

    // ========== Scenario 2: Non-affected participant (age 55, FICA $120K) ==========
    @Test
    void scenario2_nonAffectedParticipant_age55_lowEarner_getElectionOptions() {
        // Participant 2: DOB 1971-06-20, FICA $120K, 401K, has Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(2L, 2026);

        assertThat(response.getAge()).isEqualTo(55);
        assertThat(response.isAffectedBySection603()).isFalse();
        assertThat(response.isCatchUpEligible()).isTrue();
        assertThat(response.isPreTaxCatchUpAvailable()).isTrue();
        assertThat(response.isRothCatchUpAvailable()).isTrue();
        assertThat(response.getCatchUpLimit()).isEqualTo(8000.0);
        assertThat(response.getSection603Message()).isNull();
        assertThat(response.getPlanNoRothMessage()).isNull();
    }

    // ========== Scenario 3: Affected participant, plan has no Roth option ==========
    @Test
    void scenario3_affectedParticipant_planNoRoth_allCatchUpDisabled() {
        // Participant 3: DOB 1971-01-10, FICA $180K, 401K, NO Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(3L, 2026);

        assertThat(response.getAge()).isEqualTo(55);
        assertThat(response.isAffectedBySection603()).isTrue();
        assertThat(response.isCatchUpEligible()).isTrue();
        assertThat(response.isPreTaxCatchUpAvailable()).isFalse();
        assertThat(response.isRothCatchUpAvailable()).isFalse();
        assertThat(response.isPlanHasRothOption()).isFalse();
        assertThat(response.getCatchUpLimit()).isEqualTo(0.0);
        assertThat(response.getPlanNoRothMessage()).isNotNull();
        assertThat(response.getPlanNoRothMessage()).contains("not available");
        assertThat(response.getSection603Message()).isNotNull();
    }

    // ========== Scenario 4: Participant age 61, FICA $160K (super catch-up, affected) ==========
    @Test
    void scenario4_superCatchUpEligible_highEarner_rothOnly() {
        // Participant 4: DOB 1965-07-01, FICA $160K, 401K, has Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(4L, 2026);

        assertThat(response.getAge()).isEqualTo(61);
        assertThat(response.isAffectedBySection603()).isTrue();
        assertThat(response.isCatchUpEligible()).isTrue();
        assertThat(response.isSuperCatchUpEligible()).isTrue();
        assertThat(response.getCatchUpLimit()).isEqualTo(11250.0);
        assertThat(response.isPreTaxCatchUpAvailable()).isFalse();
        assertThat(response.isRothCatchUpAvailable()).isTrue();
        assertThat(response.getSection603Message()).isNotNull();
    }

    // ========== Scenario 5: Participant age 61, FICA $120K (super catch-up, non-affected) ==========
    @Test
    void scenario5_superCatchUpEligible_lowEarner_allOptionsAvailable() {
        // Participant 5: DOB 1965-04-15, FICA $120K, 401K, has Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(5L, 2026);

        assertThat(response.getAge()).isEqualTo(61);
        assertThat(response.isAffectedBySection603()).isFalse();
        assertThat(response.isCatchUpEligible()).isTrue();
        assertThat(response.isSuperCatchUpEligible()).isTrue();
        assertThat(response.getCatchUpLimit()).isEqualTo(11250.0);
        assertThat(response.isPreTaxCatchUpAvailable()).isTrue();
        assertThat(response.isRothCatchUpAvailable()).isTrue();
        assertThat(response.getSection603Message()).isNull();
    }

    // ========== Scenario 6: Participant age 45, FICA $200K (no catch-up) ==========
    @Test
    void scenario6_under50_highEarner_noCatchUp() {
        // Participant 6: DOB 1981-08-25, FICA $200K, 401K, has Roth
        ParticipantElectionOptionsResponse response =
                participantElectionService.getElectionOptions(6L, 2026);

        assertThat(response.getAge()).isEqualTo(45);
        assertThat(response.isAffectedBySection603()).isFalse();
        assertThat(response.isCatchUpEligible()).isFalse();
        assertThat(response.isSuperCatchUpEligible()).isFalse();
        assertThat(response.getCatchUpLimit()).isEqualTo(0.0);
        assertThat(response.isPreTaxCatchUpAvailable()).isFalse();
        assertThat(response.isRothCatchUpAvailable()).isFalse();
        assertThat(response.getSection603Message()).isNull();
        assertThat(response.getRegularContributionLimit()).isEqualTo(23500.0);
    }

    // ========== Scenario 10: FICA status changes mid-year ==========
    @Test
    void scenario10_ficaStatusChangeMidYear_electionOptionsUpdate() {
        // Initially participant 10 is high earner (FICA $180K)
        ParticipantElectionOptionsResponse responseBefore =
                participantElectionService.getElectionOptions(10L, 2026);
        assertThat(responseBefore.isAffectedBySection603()).isTrue();
        assertThat(responseBefore.isPreTaxCatchUpAvailable()).isFalse();

        // Update FICA wages to below threshold
        participantElectionService.updateParticipantFicaWages(10L, 140000.0);

        // Re-check - should now be non-affected
        ParticipantElectionOptionsResponse responseAfter =
                participantElectionService.getElectionOptions(10L, 2026);
        assertThat(responseAfter.isAffectedBySection603()).isFalse();
        assertThat(responseAfter.isPreTaxCatchUpAvailable()).isTrue();

        // Restore original value for other tests
        participantElectionService.updateParticipantFicaWages(10L, 180000.0);
    }

    // ========== Scenario 11: Affected participant tries pre-tax catch-up ==========
    @Test
    void scenario11_affectedParticipant_preTaxCatchUp_rejected() {
        // Participant 1: affected (age 55, FICA $180K)
        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(1L)
                .planYear(2026)
                .regularAmount(10000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(8000.0)
                .catchUpDesignation("PRE_TAX")
                .build();

        assertThatThrownBy(() -> participantElectionService.submitElection(request))
                .isInstanceOf(ElectionValidationException.class)
                .hasMessageContaining("Roth");
    }

    // ========== Election submission: successful Roth catch-up ==========
    @Test
    void submitElection_affectedParticipant_rothCatchUp_success() throws ElectionValidationException {
        // Participant 1: affected (age 55, FICA $180K) submitting Roth catch-up
        given(participantElectionRepository.findByParticipantIdAndPlanYearAndStatus(1L, 2026, "ACTIVE"))
                .willReturn(Optional.empty());
        given(participantElectionRepository.save(any(ParticipantElection.class)))
                .willAnswer(invocation -> {
                    ParticipantElection e = invocation.getArgument(0);
                    e.setId(100L);
                    return e;
                });

        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(1L)
                .planYear(2026)
                .regularAmount(15000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(8000.0)
                .catchUpDesignation("ROTH")
                .build();

        ElectionSubmissionResponse response = participantElectionService.submitElection(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getElectionId()).isEqualTo(100L);
        assertThat(response.getCatchUpLabel()).isEqualTo("Roth Catch-Up");
        assertThat(response.getErrorCode()).isNull();
    }

    // ========== Election submission: catch-up exceeds limit ==========
    @Test
    void submitElection_catchUpExceedsLimit_rejected() {
        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(2L)
                .planYear(2026)
                .regularAmount(15000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(15000.0)  // exceeds $8,000 limit
                .catchUpDesignation("ROTH")
                .build();

        assertThatThrownBy(() -> participantElectionService.submitElection(request))
                .isInstanceOf(ElectionValidationException.class)
                .hasMessageContaining("exceeds");
    }

    // ========== Election submission: no Roth plan, affected participant ==========
    @Test
    void submitElection_noRothPlan_affectedParticipant_rejected() {
        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(3L)
                .planYear(2026)
                .regularAmount(15000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(5000.0)
                .catchUpDesignation("ROTH")
                .build();

        assertThatThrownBy(() -> participantElectionService.submitElection(request))
                .isInstanceOf(ElectionValidationException.class)
                .hasMessageContaining("not available");
    }

    // ========== Election submission: supersedes existing election ==========
    @Test
    void submitElection_supersedesExistingActiveElection() throws ElectionValidationException {
        ParticipantElection existing = ParticipantElection.builder()
                .id(50L)
                .participantId(2L)
                .planYear(2026)
                .status("ACTIVE")
                .build();

        given(participantElectionRepository.findByParticipantIdAndPlanYearAndStatus(2L, 2026, "ACTIVE"))
                .willReturn(Optional.of(existing));
        given(participantElectionRepository.save(any(ParticipantElection.class)))
                .willAnswer(invocation -> {
                    ParticipantElection e = invocation.getArgument(0);
                    if (e.getId() == null) {
                        e.setId(101L);
                    }
                    return e;
                });

        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(2L)
                .planYear(2026)
                .regularAmount(12000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(5000.0)
                .catchUpDesignation("PRE_TAX")
                .build();

        ElectionSubmissionResponse response = participantElectionService.submitElection(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(existing.getStatus()).isEqualTo("SUPERSEDED");
    }

    // ========== Non-affected participant gets standard label ==========
    @Test
    void submitElection_nonAffected_preTaxCatchUp_standardLabel() throws ElectionValidationException {
        given(participantElectionRepository.findByParticipantIdAndPlanYearAndStatus(2L, 2026, "ACTIVE"))
                .willReturn(Optional.empty());
        given(participantElectionRepository.save(any(ParticipantElection.class)))
                .willAnswer(invocation -> {
                    ParticipantElection e = invocation.getArgument(0);
                    e.setId(102L);
                    return e;
                });

        ElectionSubmissionRequest request = ElectionSubmissionRequest.builder()
                .participantId(2L)
                .planYear(2026)
                .regularAmount(15000.0)
                .regularDesignation("PRE_TAX")
                .catchUpAmount(5000.0)
                .catchUpDesignation("PRE_TAX")
                .build();

        ElectionSubmissionResponse response = participantElectionService.submitElection(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCatchUpLabel()).isEqualTo("Catch-Up");
    }
}

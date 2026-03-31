package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.participant.ContributionSummaryResponse;
import com.babakjan.moneybag.entity.ContributionSummary;
import com.babakjan.moneybag.repository.ContributionSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContributionSummaryServiceTest {

    @InjectMocks
    private ContributionSummaryService contributionSummaryService;

    @Mock
    private ContributionSummaryRepository contributionSummaryRepository;

    @Test
    void getSummary_existingSummary_returnsCorrectTotals() {
        ContributionSummary summary = ContributionSummary.builder()
                .participantId(1L)
                .planYear(2026)
                .totalRegularPreTax(10000.0)
                .totalRegularRoth(5000.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(8000.0)
                .totalSuperCatchUpRoth(0.0)
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(1L, 2026))
                .willReturn(Optional.of(summary));

        ContributionSummaryResponse response = contributionSummaryService.getSummary(1L, 2026);

        assertThat(response.getParticipantId()).isEqualTo(1L);
        assertThat(response.getPlanYear()).isEqualTo(2026);
        assertThat(response.getRegularPreTax()).isEqualTo(10000.0);
        assertThat(response.getRegularRoth()).isEqualTo(5000.0);
        assertThat(response.getCatchUpRoth()).isEqualTo(8000.0);
        assertThat(response.getTotalContributions()).isEqualTo(23000.0);
    }

    @Test
    void getSummary_noSummary_returnsZeros() {
        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(999L, 2026))
                .willReturn(Optional.empty());

        ContributionSummaryResponse response = contributionSummaryService.getSummary(999L, 2026);

        assertThat(response.getParticipantId()).isEqualTo(999L);
        assertThat(response.getTotalContributions()).isEqualTo(0.0);
    }

    @Test
    void updateSummary_regularPreTax_updatesCorrectly() {
        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(1L, 2026))
                .willReturn(Optional.empty());
        given(contributionSummaryRepository.save(any(ContributionSummary.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        contributionSummaryService.updateSummary(1L, 2026, 5000.0, "REGULAR", "PRE_TAX");

        verify(contributionSummaryRepository).save(any(ContributionSummary.class));
    }

    @Test
    void updateSummary_catchUpRoth_updatesCorrectly() {
        ContributionSummary existing = ContributionSummary.builder()
                .participantId(1L)
                .planYear(2026)
                .totalRegularPreTax(10000.0)
                .totalRegularRoth(0.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(3000.0)
                .totalSuperCatchUpRoth(0.0)
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(1L, 2026))
                .willReturn(Optional.of(existing));
        given(contributionSummaryRepository.save(any(ContributionSummary.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        contributionSummaryService.updateSummary(1L, 2026, 2000.0, "CATCH_UP", "ROTH");

        assertThat(existing.getTotalCatchUpRoth()).isEqualTo(5000.0);
    }

    @Test
    void updateSummary_superCatchUp_updatesCorrectly() {
        ContributionSummary existing = ContributionSummary.builder()
                .participantId(1L)
                .planYear(2026)
                .totalRegularPreTax(0.0)
                .totalRegularRoth(0.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(0.0)
                .totalSuperCatchUpRoth(5000.0)
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(1L, 2026))
                .willReturn(Optional.of(existing));
        given(contributionSummaryRepository.save(any(ContributionSummary.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        contributionSummaryService.updateSummary(1L, 2026, 3000.0, "SUPER_CATCH_UP", "ROTH");

        assertThat(existing.getTotalSuperCatchUpRoth()).isEqualTo(8000.0);
    }
}

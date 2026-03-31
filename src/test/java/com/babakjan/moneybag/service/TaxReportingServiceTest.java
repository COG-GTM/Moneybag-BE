package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.tax.TaxReportResponse;
import com.babakjan.moneybag.dto.tax.YearEndTaxSummaryResponse;
import com.babakjan.moneybag.entity.ContributionSummary;
import com.babakjan.moneybag.entity.TaxReportEntry;
import com.babakjan.moneybag.error.exception.TaxReportingException;
import com.babakjan.moneybag.repository.ContributionSummaryRepository;
import com.babakjan.moneybag.repository.TaxReportEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TaxReportingServiceTest {

    @InjectMocks
    private TaxReportingService taxReportingService;

    @Mock
    private TaxReportEntryRepository taxReportEntryRepository;

    @Mock
    private ContributionSummaryRepository contributionSummaryRepository;

    // ========== Scenario 7: Affected participant contributes $8,000 Roth catch-up to 403(b) ==========
    @Test
    void scenario7_rothCatchUp403b_w2Box12CodeBB() throws TaxReportingException {
        ContributionSummary summary = ContributionSummary.builder()
                .participantId(7L)
                .planYear(2026)
                .totalRegularPreTax(0.0)
                .totalRegularRoth(0.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(8000.0)
                .totalSuperCatchUpRoth(0.0)
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(7L, 2026))
                .willReturn(Optional.of(summary));

        TaxReportEntry existingEntry = TaxReportEntry.builder()
                .planType("403B")
                .build();
        given(taxReportEntryRepository.findByParticipantIdAndPlanYear(7L, 2026))
                .willReturn(List.of(existingEntry));
        given(taxReportEntryRepository.save(any(TaxReportEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TaxReportResponse response = taxReportingService.generateTaxReport(7L, 2026);

        assertThat(response.getW2Box12Code()).isEqualTo("BB");
        assertThat(response.getW2Box12Amount()).isEqualTo(8000.0);
        assertThat(response.getCatchUpRothTotal()).isEqualTo(8000.0);
        assertThat(response.getDistributionCode1099R()).isEqualTo("B");
    }

    // ========== Scenario 8: $5K regular Roth + $8K Roth catch-up shown separately ==========
    @Test
    void scenario8_regularRothPlusCatchUp_shownSeparately() throws TaxReportingException {
        TaxReportEntry entry = TaxReportEntry.builder()
                .participantId(8L)
                .planYear(2026)
                .planType("401K")
                .regularRothAmount(5000.0)
                .regularPreTaxAmount(0.0)
                .catchUpRothAmount(8000.0)
                .catchUpPreTaxAmount(0.0)
                .superCatchUpRothAmount(0.0)
                .w2Box12Code("AA")
                .w2Box12Amount(8000.0)
                .build();

        given(taxReportEntryRepository.findByParticipantIdAndPlanYear(8L, 2026))
                .willReturn(List.of(entry));

        YearEndTaxSummaryResponse response = taxReportingService.generateYearEndSummary(8L, 2026);

        assertThat(response.getParticipantId()).isEqualTo(8L);
        assertThat(response.getPlanYear()).isEqualTo(2026);
        assertThat(response.getTotalRegularRoth()).isEqualTo(5000.0);
        assertThat(response.getTotalRothCatchUp()).isEqualTo(8000.0);
        assertThat(response.getGrandTotal()).isEqualTo(13000.0);

        // Verify entries show both amounts separately
        assertThat(response.getEntries()).hasSize(1);
        assertThat(response.getEntries().get(0).getRegularRothTotal()).isEqualTo(5000.0);
        assertThat(response.getEntries().get(0).getCatchUpRothTotal()).isEqualTo(8000.0);
    }

    // ========== Scenario 9: Super catch-up $11,250 Roth to 401(k), W-2 Box 12 Code AA ==========
    @Test
    void scenario9_superCatchUp401k_w2Box12CodeAA() throws TaxReportingException {
        ContributionSummary summary = ContributionSummary.builder()
                .participantId(9L)
                .planYear(2026)
                .totalRegularPreTax(0.0)
                .totalRegularRoth(0.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(0.0)
                .totalSuperCatchUpRoth(11250.0)
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(9L, 2026))
                .willReturn(Optional.of(summary));
        given(taxReportEntryRepository.findByParticipantIdAndPlanYear(9L, 2026))
                .willReturn(List.of());
        given(taxReportEntryRepository.save(any(TaxReportEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TaxReportResponse response = taxReportingService.generateTaxReport(9L, 2026);

        assertThat(response.getW2Box12Code()).isEqualTo("AA");
        assertThat(response.getW2Box12Amount()).isEqualTo(11250.0);
        assertThat(response.getSuperCatchUpRothTotal()).isEqualTo(11250.0);
        assertThat(response.getDistributionCode1099R()).isEqualTo("B");
    }

    // ========== Tax report: governmental 457b uses EE code ==========
    @Test
    void taxReport_governmental457b_usesEECode() throws TaxReportingException {
        ContributionSummary summary = ContributionSummary.builder()
                .participantId(100L)
                .planYear(2026)
                .totalRegularPreTax(5000.0)
                .totalRegularRoth(3000.0)
                .totalCatchUpPreTax(0.0)
                .totalCatchUpRoth(4000.0)
                .totalSuperCatchUpRoth(0.0)
                .build();

        TaxReportEntry existingEntry = TaxReportEntry.builder()
                .planType("GOVERNMENTAL_457B")
                .build();

        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(100L, 2026))
                .willReturn(Optional.of(summary));
        given(taxReportEntryRepository.findByParticipantIdAndPlanYear(100L, 2026))
                .willReturn(List.of(existingEntry));
        given(taxReportEntryRepository.save(any(TaxReportEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TaxReportResponse response = taxReportingService.generateTaxReport(100L, 2026);

        assertThat(response.getW2Box12Code()).isEqualTo("EE");
        assertThat(response.getW2Box12Amount()).isEqualTo(4000.0);
    }

    // ========== Year-end summary with no data ==========
    @Test
    void yearEndSummary_noData_returnsEmptySummary() throws TaxReportingException {
        given(taxReportEntryRepository.findByParticipantIdAndPlanYear(999L, 2026))
                .willReturn(List.of());
        given(contributionSummaryRepository.findByParticipantIdAndPlanYear(999L, 2026))
                .willReturn(Optional.empty());

        YearEndTaxSummaryResponse response = taxReportingService.generateYearEndSummary(999L, 2026);

        assertThat(response.getParticipantId()).isEqualTo(999L);
        assertThat(response.getGrandTotal()).isEqualTo(0.0);
        assertThat(response.getEntries()).isEmpty();
    }
}

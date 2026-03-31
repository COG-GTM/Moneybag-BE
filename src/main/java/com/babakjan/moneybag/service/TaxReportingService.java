package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.tax.TaxReportResponse;
import com.babakjan.moneybag.dto.tax.YearEndTaxSummaryResponse;
import com.babakjan.moneybag.entity.ContributionSummary;
import com.babakjan.moneybag.entity.TaxReportEntry;
import com.babakjan.moneybag.error.exception.TaxReportingException;
import com.babakjan.moneybag.repository.ContributionSummaryRepository;
import com.babakjan.moneybag.repository.TaxReportEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaxReportingService {

    private final TaxReportEntryRepository taxReportEntryRepository;
    private final ContributionSummaryRepository contributionSummaryRepository;

    public TaxReportResponse generateTaxReport(Long participantId, int planYear) throws TaxReportingException {
        Optional<ContributionSummary> optSummary = contributionSummaryRepository
                .findByParticipantIdAndPlanYear(participantId, planYear);

        if (optSummary.isEmpty()) {
            throw new TaxReportingException("No contribution summary found for participant " + participantId
                    + " and plan year " + planYear);
        }

        ContributionSummary summary = optSummary.get();

        // Determine plan type from existing tax report entries or use default
        List<TaxReportEntry> entries = taxReportEntryRepository
                .findByParticipantIdAndPlanYear(participantId, planYear);

        String planType = "401K";
        if (!entries.isEmpty()) {
            planType = entries.get(0).getPlanType();
        }

        String w2Box12Code = getW2Box12Code(planType);
        double totalRothCatchUp = safeDouble(summary.getTotalCatchUpRoth())
                + safeDouble(summary.getTotalSuperCatchUpRoth());
        double w2Box12Amount = safeDouble(summary.getTotalRegularRoth()) + totalRothCatchUp;

        // 1099-R distribution code for Roth catch-up
        String distributionCode1099R = totalRothCatchUp > 0 ? "B" : null;

        // Upsert tax report entry: update existing or create new
        TaxReportEntry entry;
        if (!entries.isEmpty()) {
            entry = entries.get(0);
        } else {
            entry = new TaxReportEntry();
            entry.setParticipantId(participantId);
            entry.setPlanYear(planYear);
        }
        entry.setPlanType(planType);
        entry.setRegularRothAmount(safeDouble(summary.getTotalRegularRoth()));
        entry.setRegularPreTaxAmount(safeDouble(summary.getTotalRegularPreTax()));
        entry.setCatchUpRothAmount(safeDouble(summary.getTotalCatchUpRoth()));
        entry.setCatchUpPreTaxAmount(safeDouble(summary.getTotalCatchUpPreTax()));
        entry.setSuperCatchUpRothAmount(safeDouble(summary.getTotalSuperCatchUpRoth()));
        entry.setW2Box12Code(w2Box12Code);
        entry.setW2Box12Amount(w2Box12Amount);
        taxReportEntryRepository.save(entry);

        return TaxReportResponse.builder()
                .participantId(participantId)
                .planYear(planYear)
                .planType(planType)
                .regularRothTotal(safeDouble(summary.getTotalRegularRoth()))
                .regularPreTaxTotal(safeDouble(summary.getTotalRegularPreTax()))
                .catchUpRothTotal(safeDouble(summary.getTotalCatchUpRoth()))
                .catchUpPreTaxTotal(safeDouble(summary.getTotalCatchUpPreTax()))
                .superCatchUpRothTotal(safeDouble(summary.getTotalSuperCatchUpRoth()))
                .w2Box12Code(w2Box12Code)
                .w2Box12Amount(w2Box12Amount)
                .distributionCode1099R(distributionCode1099R)
                .build();
    }

    public YearEndTaxSummaryResponse generateYearEndSummary(Long participantId, int planYear)
            throws TaxReportingException {
        List<TaxReportEntry> entries = taxReportEntryRepository
                .findByParticipantIdAndPlanYear(participantId, planYear);

        List<TaxReportResponse> entryResponses = new ArrayList<>();
        double totalRothCatchUp = 0.0;
        double totalRegularRoth = 0.0;
        double totalPreTax = 0.0;

        for (TaxReportEntry entry : entries) {
            double entryRothCatchUp = safeDouble(entry.getCatchUpRothAmount())
                    + safeDouble(entry.getSuperCatchUpRothAmount());
            totalRothCatchUp += entryRothCatchUp;
            totalRegularRoth += safeDouble(entry.getRegularRothAmount());
            totalPreTax += safeDouble(entry.getRegularPreTaxAmount())
                    + safeDouble(entry.getCatchUpPreTaxAmount());

            String distributionCode1099R = entryRothCatchUp > 0 ? "B" : null;

            entryResponses.add(TaxReportResponse.builder()
                    .participantId(participantId)
                    .planYear(planYear)
                    .planType(entry.getPlanType())
                    .regularRothTotal(safeDouble(entry.getRegularRothAmount()))
                    .regularPreTaxTotal(safeDouble(entry.getRegularPreTaxAmount()))
                    .catchUpRothTotal(safeDouble(entry.getCatchUpRothAmount()))
                    .catchUpPreTaxTotal(safeDouble(entry.getCatchUpPreTaxAmount()))
                    .superCatchUpRothTotal(safeDouble(entry.getSuperCatchUpRothAmount()))
                    .w2Box12Code(entry.getW2Box12Code())
                    .w2Box12Amount(entry.getW2Box12Amount())
                    .distributionCode1099R(distributionCode1099R)
                    .build());
        }

        // If no entries exist, also check contribution summary
        if (entries.isEmpty()) {
            Optional<ContributionSummary> optSummary = contributionSummaryRepository
                    .findByParticipantIdAndPlanYear(participantId, planYear);
            if (optSummary.isPresent()) {
                ContributionSummary summary = optSummary.get();
                totalRegularRoth = safeDouble(summary.getTotalRegularRoth());
                totalPreTax = safeDouble(summary.getTotalRegularPreTax())
                        + safeDouble(summary.getTotalCatchUpPreTax());
                totalRothCatchUp = safeDouble(summary.getTotalCatchUpRoth())
                        + safeDouble(summary.getTotalSuperCatchUpRoth());
            }
        }

        double grandTotal = totalRothCatchUp + totalRegularRoth + totalPreTax;

        return YearEndTaxSummaryResponse.builder()
                .participantId(participantId)
                .planYear(planYear)
                .entries(entryResponses)
                .totalRothCatchUp(totalRothCatchUp)
                .totalRegularRoth(totalRegularRoth)
                .totalPreTax(totalPreTax)
                .grandTotal(grandTotal)
                .build();
    }

    private String getW2Box12Code(String planType) {
        return switch (planType) {
            case "403B" -> "BB";
            case "GOVERNMENTAL_457B" -> "EE";
            default -> "AA"; // 401K
        };
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}

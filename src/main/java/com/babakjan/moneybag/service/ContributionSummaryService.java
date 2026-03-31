package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.participant.ContributionSummaryResponse;
import com.babakjan.moneybag.entity.ContributionSummary;
import com.babakjan.moneybag.repository.ContributionSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContributionSummaryService {

    private final ContributionSummaryRepository contributionSummaryRepository;

    public ContributionSummaryResponse getSummary(Long participantId, int planYear) {
        Optional<ContributionSummary> optSummary = contributionSummaryRepository
                .findByParticipantIdAndPlanYear(participantId, planYear);

        if (optSummary.isEmpty()) {
            return ContributionSummaryResponse.builder()
                    .participantId(participantId)
                    .planYear(planYear)
                    .regularPreTax(0.0)
                    .regularRoth(0.0)
                    .catchUpPreTax(0.0)
                    .catchUpRoth(0.0)
                    .superCatchUpRoth(0.0)
                    .totalContributions(0.0)
                    .build();
        }

        ContributionSummary summary = optSummary.get();
        double total = safeDouble(summary.getTotalRegularPreTax())
                + safeDouble(summary.getTotalRegularRoth())
                + safeDouble(summary.getTotalCatchUpPreTax())
                + safeDouble(summary.getTotalCatchUpRoth())
                + safeDouble(summary.getTotalSuperCatchUpRoth());

        return ContributionSummaryResponse.builder()
                .participantId(participantId)
                .planYear(planYear)
                .regularPreTax(safeDouble(summary.getTotalRegularPreTax()))
                .regularRoth(safeDouble(summary.getTotalRegularRoth()))
                .catchUpPreTax(safeDouble(summary.getTotalCatchUpPreTax()))
                .catchUpRoth(safeDouble(summary.getTotalCatchUpRoth()))
                .superCatchUpRoth(safeDouble(summary.getTotalSuperCatchUpRoth()))
                .totalContributions(total)
                .build();
    }

    public void updateSummary(Long participantId, int planYear, Double amount, String type, String designation) {
        ContributionSummary summary = contributionSummaryRepository
                .findByParticipantIdAndPlanYear(participantId, planYear)
                .orElse(ContributionSummary.builder()
                        .participantId(participantId)
                        .planYear(planYear)
                        .totalRegularPreTax(0.0)
                        .totalRegularRoth(0.0)
                        .totalCatchUpPreTax(0.0)
                        .totalCatchUpRoth(0.0)
                        .totalSuperCatchUpRoth(0.0)
                        .build());

        switch (type) {
            case "REGULAR" -> {
                if ("PRE_TAX".equals(designation)) {
                    summary.setTotalRegularPreTax(safeDouble(summary.getTotalRegularPreTax()) + amount);
                } else {
                    summary.setTotalRegularRoth(safeDouble(summary.getTotalRegularRoth()) + amount);
                }
            }
            case "CATCH_UP" -> {
                if ("PRE_TAX".equals(designation)) {
                    summary.setTotalCatchUpPreTax(safeDouble(summary.getTotalCatchUpPreTax()) + amount);
                } else {
                    summary.setTotalCatchUpRoth(safeDouble(summary.getTotalCatchUpRoth()) + amount);
                }
            }
            case "SUPER_CATCH_UP" -> {
                summary.setTotalSuperCatchUpRoth(safeDouble(summary.getTotalSuperCatchUpRoth()) + amount);
            }
            default -> throw new IllegalArgumentException("Unknown contribution type: " + type);
        }

        contributionSummaryRepository.save(summary);
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}

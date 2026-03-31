package com.babakjan.moneybag.dto.participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionSummaryResponse {
    private Long participantId;
    private int planYear;
    private Double regularPreTax;
    private Double regularRoth;
    private Double catchUpPreTax;
    private Double catchUpRoth;
    private Double superCatchUpRoth;
    private Double totalContributions;
}

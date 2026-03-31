package com.babakjan.moneybag.dto.contribution;

import com.babakjan.moneybag.entity.ContributionDesignation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionEligibilityResponse {
    private Long participantId;
    private int planYear;
    private boolean isHighEarner;
    private boolean isCatchUpEligible;
    private boolean isSuperCatchUpEligible;
    private Double catchUpLimit;
    private ContributionDesignation requiredDesignation;
    private String errorCode;
    private String errorMessage;
}

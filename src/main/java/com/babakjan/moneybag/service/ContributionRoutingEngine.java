package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.contribution.ContributionEligibilityResponse;
import com.babakjan.moneybag.dto.contribution.ContributionRequest;
import com.babakjan.moneybag.dto.contribution.ContributionResponse;
import com.babakjan.moneybag.entity.ContributionDesignation;
import com.babakjan.moneybag.entity.ContributionType;
import com.babakjan.moneybag.error.exception.ContributionRoutingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContributionRoutingEngine {

    private final ContributionEligibilityService eligibilityService;

    /**
     * Route a contribution request based on SECURE 2.0 Section 603 rules.
     *
     * @param request the contribution request
     * @return routing response indicating acceptance or rejection
     * @throws ContributionRoutingException if eligibility determination fails
     */
    public ContributionResponse routeContribution(ContributionRequest request) throws ContributionRoutingException {
        ContributionEligibilityResponse eligibility = eligibilityService.determineEligibility(
                request.getParticipantId(), request.getPlanYear());

        // Regular contributions: Section 603 doesn't apply
        if (request.getContributionType() == ContributionType.REGULAR) {
            return ContributionResponse.builder()
                    .accepted(true)
                    .build();
        }

        // Participant must be catch-up eligible (age 50+)
        if (!eligibility.isCatchUpEligible()) {
            return ContributionResponse.builder()
                    .accepted(false)
                    .errorCode("CATCHUP_NOT_ELIGIBLE")
                    .errorMessage("Participant is not eligible for catch-up contributions")
                    .build();
        }

        // For SUPER_CATCHUP type: must be in 60-63 age range
        if (request.getContributionType() == ContributionType.SUPER_CATCHUP
                && !eligibility.isSuperCatchUpEligible()) {
            return ContributionResponse.builder()
                    .accepted(false)
                    .errorCode("CATCHUP_NOT_ELIGIBLE")
                    .errorMessage("Participant is not eligible for super catch-up contributions")
                    .build();
        }

        // Determine the applicable limit
        Double applicableLimit = eligibility.getCatchUpLimit();
        if (applicableLimit != null && request.getAmount() != null && request.getAmount() > applicableLimit) {
            return ContributionResponse.builder()
                    .accepted(false)
                    .errorCode("CONTRIBUTION_LIMIT_EXCEEDED")
                    .errorMessage("Contribution amount exceeds applicable limit")
                    .build();
        }

        // Check Roth requirement for high earners
        if (eligibility.getRequiredDesignation() == ContributionDesignation.ROTH
                && request.getDesignation() != ContributionDesignation.ROTH) {
            return ContributionResponse.builder()
                    .accepted(false)
                    .errorCode("ROTH_CATCHUP_REQUIRED")
                    .errorMessage("Catch-up contributions must be designated Roth per SECURE 2.0 Section 603")
                    .build();
        }

        // Contribution accepted
        return ContributionResponse.builder()
                .accepted(true)
                .build();
    }
}

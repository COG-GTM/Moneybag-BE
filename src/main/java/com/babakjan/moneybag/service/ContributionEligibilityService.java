package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.contribution.ContributionEligibilityResponse;
import com.babakjan.moneybag.entity.ContributionDesignation;
import com.babakjan.moneybag.entity.ContributionLimitConfig;
import com.babakjan.moneybag.entity.ParticipantProfile;
import com.babakjan.moneybag.entity.PlanType;
import com.babakjan.moneybag.error.exception.ContributionRoutingException;
import com.babakjan.moneybag.repository.ContributionLimitConfigRepository;
import com.babakjan.moneybag.repository.ParticipantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContributionEligibilityService {

    private final ParticipantProfileRepository participantProfileRepository;
    private final ContributionLimitConfigRepository contributionLimitConfigRepository;
    private final AgeCalculationService ageCalculationService;

    /**
     * Determine a participant's eligibility for catch-up contributions per SECURE 2.0 Section 603.
     *
     * @param participantId the participant profile ID
     * @param planYear the plan year
     * @return eligibility response with catch-up tier, Roth requirement, and applicable limit
     * @throws ContributionRoutingException if participant or config not found
     */
    public ContributionEligibilityResponse determineEligibility(Long participantId, int planYear)
            throws ContributionRoutingException {

        Optional<ParticipantProfile> optionalProfile = participantProfileRepository.findById(participantId);
        if (optionalProfile.isEmpty()) {
            throw new ContributionRoutingException("PARTICIPANT_NOT_FOUND",
                    "Participant profile not found for id: " + participantId);
        }
        ParticipantProfile profile = optionalProfile.get();

        Optional<ContributionLimitConfig> optionalConfig = contributionLimitConfigRepository.findByPlanYear(planYear);
        if (optionalConfig.isEmpty()) {
            throw new ContributionRoutingException("CONFIG_NOT_FOUND",
                    "Contribution limit configuration not found for plan year: " + planYear);
        }
        ContributionLimitConfig config = optionalConfig.get();

        // Aggregate FICA wages including controlled group
        double totalFicaWages = aggregateFicaWages(profile);

        // Determine high earner status: "in excess of" means strictly greater than threshold
        boolean isHighEarner = totalFicaWages > config.getFicaWageThreshold();

        // For new hires with no prior-year W-2: good-faith compliance allows pre-tax
        if (Boolean.TRUE.equals(profile.getIsNewHire()) && profile.getPriorYearFicaWages() == null) {
            isHighEarner = false;
        }

        // Determine catch-up eligibility tiers
        boolean isCatchUpEligible = ageCalculationService.isCatchUpEligible(
                profile.getDateOfBirth(), planYear);
        boolean isSuperCatchUpEligible = ageCalculationService.isSuperCatchUpEligible(
                profile.getDateOfBirth(), planYear);

        // Determine applicable catch-up limit
        Double catchUpLimit = null;
        if (isSuperCatchUpEligible) {
            catchUpLimit = config.getSuperCatchUpLimit();
        } else if (isCatchUpEligible) {
            catchUpLimit = config.getStandardCatchUpLimit();
        }

        // Determine required designation
        ContributionDesignation requiredDesignation = null;
        if (isCatchUpEligible && isHighEarner) {
            // Check for governmental 457(b) special catch-up exemption
            if (profile.getEmployerPlanType() == PlanType.GOVERNMENTAL_457B
                    && Boolean.TRUE.equals(profile.getSpecialCatchUpEligible())) {
                // Special catch-up amounts are exempt from Roth requirement
                // requiredDesignation stays null (participant can choose)
                requiredDesignation = null;
            } else {
                requiredDesignation = ContributionDesignation.ROTH;
            }
        }

        return ContributionEligibilityResponse.builder()
                .participantId(participantId)
                .planYear(planYear)
                .isHighEarner(isHighEarner)
                .isCatchUpEligible(isCatchUpEligible)
                .isSuperCatchUpEligible(isSuperCatchUpEligible)
                .catchUpLimit(catchUpLimit)
                .requiredDesignation(requiredDesignation)
                .build();
    }

    /**
     * Aggregate FICA wages including controlled group wages.
     */
    private double aggregateFicaWages(ParticipantProfile profile) {
        double wages = 0.0;
        if (profile.getPriorYearFicaWages() != null) {
            wages += profile.getPriorYearFicaWages();
        }
        if (profile.getFicaWagesFromControlledGroup() != null) {
            wages += profile.getFicaWagesFromControlledGroup();
        }
        return wages;
    }
}

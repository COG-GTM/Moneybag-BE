package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.participant.ElectionSubmissionRequest;
import com.babakjan.moneybag.dto.participant.ElectionSubmissionResponse;
import com.babakjan.moneybag.dto.participant.ParticipantElectionOptionsResponse;
import com.babakjan.moneybag.entity.ParticipantElection;
import com.babakjan.moneybag.error.exception.ElectionValidationException;
import com.babakjan.moneybag.repository.ParticipantElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ParticipantElectionService {

    private final ParticipantElectionRepository participantElectionRepository;

    private static final double STANDARD_CATCH_UP_LIMIT = 8000.0;
    private static final double SUPER_CATCH_UP_LIMIT = 11250.0;
    private static final double REGULAR_CONTRIBUTION_LIMIT = 23500.0;
    private static final double FICA_THRESHOLD_2026 = 150000.0;
    private static final String SECTION_603_MESSAGE =
            "Based on your prior-year compensation, catch-up contributions must be made on an after-tax (Roth) basis per the SECURE 2.0 Act";
    private static final String NO_ROTH_MESSAGE =
            "Catch-up contributions are not available in your current plan. Contact your plan sponsor for more information.";
    private static final String LEARN_MORE_URL =
            "https://www.irs.gov/retirement-plans/secure-2-0-act-section-603";

    // Stub participant data store for demonstration
    private static final Map<Long, ParticipantProfile> PARTICIPANT_PROFILES = new HashMap<>();

    static {
        // Participant 1: age 55, high earner, plan has Roth
        PARTICIPANT_PROFILES.put(1L, new ParticipantProfile(
                LocalDate.of(1971, 3, 15), 180000.0, "401K", true));
        // Participant 2: age 55, non-affected
        PARTICIPANT_PROFILES.put(2L, new ParticipantProfile(
                LocalDate.of(1971, 6, 20), 120000.0, "401K", true));
        // Participant 3: affected, plan has NO Roth
        PARTICIPANT_PROFILES.put(3L, new ParticipantProfile(
                LocalDate.of(1971, 1, 10), 180000.0, "401K", false));
        // Participant 4: age 61, high earner (super catch-up eligible)
        PARTICIPANT_PROFILES.put(4L, new ParticipantProfile(
                LocalDate.of(1965, 7, 1), 160000.0, "401K", true));
        // Participant 5: age 61, non-affected (super catch-up eligible)
        PARTICIPANT_PROFILES.put(5L, new ParticipantProfile(
                LocalDate.of(1965, 4, 15), 120000.0, "401K", true));
        // Participant 6: age 45, high earner (no catch-up)
        PARTICIPANT_PROFILES.put(6L, new ParticipantProfile(
                LocalDate.of(1981, 8, 25), 200000.0, "401K", true));
        // Participant 7: age 55, high earner, 403b plan
        PARTICIPANT_PROFILES.put(7L, new ParticipantProfile(
                LocalDate.of(1971, 2, 10), 180000.0, "403B", true));
        // Participant 8: age 55, high earner, 401k plan (for mixed contributions)
        PARTICIPANT_PROFILES.put(8L, new ParticipantProfile(
                LocalDate.of(1971, 5, 5), 180000.0, "401K", true));
        // Participant 9: age 61, high earner, 401k (super catch-up)
        PARTICIPANT_PROFILES.put(9L, new ParticipantProfile(
                LocalDate.of(1965, 9, 12), 160000.0, "401K", true));
        // Participant 10: FICA status change participant
        PARTICIPANT_PROFILES.put(10L, new ParticipantProfile(
                LocalDate.of(1971, 11, 30), 180000.0, "401K", true));
    }

    public ParticipantElectionOptionsResponse getElectionOptions(Long participantId, int planYear) {
        ParticipantProfile profile = PARTICIPANT_PROFILES.getOrDefault(participantId,
                new ParticipantProfile(LocalDate.of(1985, 1, 1), 100000.0, "401K", true));

        int age = calculateAgeInYear(profile.dateOfBirth(), planYear);
        boolean isHighEarner = profile.priorYearFicaWages() > FICA_THRESHOLD_2026;
        boolean isCatchUpEligible = age >= 50;
        boolean isSuperCatchUpEligible = age >= 60 && age <= 63;
        boolean isAffected = isCatchUpEligible && isHighEarner;

        double catchUpLimit = 0.0;
        if (isSuperCatchUpEligible) {
            catchUpLimit = SUPER_CATCH_UP_LIMIT;
        } else if (isCatchUpEligible) {
            catchUpLimit = STANDARD_CATCH_UP_LIMIT;
        }

        boolean preTaxCatchUpAvailable = isCatchUpEligible && !isAffected;
        boolean rothCatchUpAvailable = isCatchUpEligible && profile.planHasRothOption();

        String section603Message = null;
        String planNoRothMessage = null;

        if (isAffected) {
            section603Message = SECTION_603_MESSAGE;
            if (!profile.planHasRothOption()) {
                planNoRothMessage = NO_ROTH_MESSAGE;
                rothCatchUpAvailable = false;
                preTaxCatchUpAvailable = false;
                catchUpLimit = 0.0;
            }
        }

        return ParticipantElectionOptionsResponse.builder()
                .participantId(participantId)
                .planYear(planYear)
                .age(age)
                .isAffectedBySection603(isAffected)
                .isCatchUpEligible(isCatchUpEligible)
                .isSuperCatchUpEligible(isSuperCatchUpEligible)
                .catchUpLimit(catchUpLimit)
                .regularContributionLimit(REGULAR_CONTRIBUTION_LIMIT)
                .preTaxCatchUpAvailable(preTaxCatchUpAvailable)
                .rothCatchUpAvailable(rothCatchUpAvailable)
                .planHasRothOption(profile.planHasRothOption())
                .section603Message(section603Message)
                .planNoRothMessage(planNoRothMessage)
                .learnMoreUrl(LEARN_MORE_URL)
                .build();
    }

    public ElectionSubmissionResponse submitElection(ElectionSubmissionRequest request)
            throws ElectionValidationException {
        ParticipantProfile profile = PARTICIPANT_PROFILES.getOrDefault(request.getParticipantId(),
                new ParticipantProfile(LocalDate.of(1985, 1, 1), 100000.0, "401K", true));

        int age = calculateAgeInYear(profile.dateOfBirth(), request.getPlanYear());
        boolean isHighEarner = profile.priorYearFicaWages() > FICA_THRESHOLD_2026;
        boolean isCatchUpEligible = age >= 50;
        boolean isSuperCatchUpEligible = age >= 60 && age <= 63;
        boolean isAffected = isCatchUpEligible && isHighEarner;

        // Validate: affected participant cannot use PRE_TAX catch-up
        if (isAffected && request.getCatchUpAmount() != null && request.getCatchUpAmount() > 0
                && "PRE_TAX".equals(request.getCatchUpDesignation())) {
            throw new ElectionValidationException("PRETAX_CATCHUP_NOT_ALLOWED",
                    "Affected participants must designate catch-up contributions as Roth per SECURE 2.0 Section 603");
        }

        // Validate: plan has no Roth option and participant is affected
        if (isAffected && !profile.planHasRothOption()
                && request.getCatchUpAmount() != null && request.getCatchUpAmount() > 0) {
            throw new ElectionValidationException("NO_ROTH_CATCHUP_AVAILABLE",
                    NO_ROTH_MESSAGE);
        }

        // Validate: catch-up amount doesn't exceed limit
        double applicableLimit = isSuperCatchUpEligible ? SUPER_CATCH_UP_LIMIT : STANDARD_CATCH_UP_LIMIT;
        if (request.getCatchUpAmount() != null && request.getCatchUpAmount() > applicableLimit) {
            throw new ElectionValidationException("CATCHUP_LIMIT_EXCEEDED",
                    "Catch-up contribution amount exceeds the applicable limit of $" + applicableLimit);
        }

        // Supersede any existing active election
        participantElectionRepository
                .findByParticipantIdAndPlanYearAndStatus(request.getParticipantId(), request.getPlanYear(), "ACTIVE")
                .ifPresent(existing -> {
                    existing.setStatus("SUPERSEDED");
                    existing.setUpdatedAt(new Date());
                    participantElectionRepository.save(existing);
                });

        // Determine catch-up type
        String catchUpType = null;
        if (request.getCatchUpAmount() != null && request.getCatchUpAmount() > 0) {
            catchUpType = isSuperCatchUpEligible ? "SUPER" : "STANDARD";
        }

        // Create new election
        ParticipantElection election = ParticipantElection.builder()
                .participantId(request.getParticipantId())
                .planYear(request.getPlanYear())
                .regularContributionAmount(request.getRegularAmount())
                .regularDesignation(request.getRegularDesignation())
                .catchUpContributionAmount(request.getCatchUpAmount())
                .catchUpDesignation(request.getCatchUpDesignation())
                .catchUpType(catchUpType)
                .electionDate(new Date())
                .status("ACTIVE")
                .build();

        ParticipantElection saved = participantElectionRepository.save(election);

        // Determine catch-up label
        String catchUpLabel = null;
        if (request.getCatchUpAmount() != null && request.getCatchUpAmount() > 0) {
            catchUpLabel = "ROTH".equals(request.getCatchUpDesignation()) ? "Roth Catch-Up" : "Catch-Up";
        }

        return ElectionSubmissionResponse.builder()
                .success(true)
                .electionId(saved.getId())
                .catchUpLabel(catchUpLabel)
                .projectedAnnualTaxImpact("Roth contributions are made with after-tax dollars and grow tax-free")
                .build();
    }

    /**
     * Update the FICA wages for a participant (simulates mid-year status change).
     */
    public void updateParticipantFicaWages(Long participantId, double newFicaWages) {
        ParticipantProfile existing = PARTICIPANT_PROFILES.get(participantId);
        if (existing != null) {
            PARTICIPANT_PROFILES.put(participantId, new ParticipantProfile(
                    existing.dateOfBirth(), newFicaWages, existing.planType(), existing.planHasRothOption()));
        }
    }

    private int calculateAgeInYear(LocalDate dateOfBirth, int planYear) {
        return planYear - dateOfBirth.getYear();
    }

    // Stub participant profile record
    public record ParticipantProfile(
            LocalDate dateOfBirth,
            double priorYearFicaWages,
            String planType,
            boolean planHasRothOption
    ) {}
}

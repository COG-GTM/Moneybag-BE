package com.babakjan.moneybag.service;

import com.babakjan.moneybag.entity.*;
import com.babakjan.moneybag.error.exception.ContributionValidationException;
import com.babakjan.moneybag.repository.ContributionLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContributionValidationService {

    private final ContributionLimitRepository contributionLimitRepository;

    /**
     * Main entry point that orchestrates all validation checks for a contribution.
     * @param contribution the contribution to validate
     * @throws ContributionValidationException if the contribution violates SECURE 2.0 rules
     */
    public void validateContribution(Contribution contribution) throws ContributionValidationException {
        if (contribution.getContributionType() == ContributionType.REGULAR) {
            return;
        }

        Participant participant = contribution.getParticipant();
        int taxYear = contribution.getPayrollDate().getYear();

        if (!isCatchUpEligible(participant, taxYear)) {
            throw new ContributionValidationException(
                    "Participant is not eligible for catch-up contributions. Must be age 50 or older by December 31 of the tax year.");
        }

        enforceTaxTreatment(contribution);
    }

    /**
     * Calculate participant's age as of a given date.
     * @param dateOfBirth participant's date of birth
     * @param asOfDate the date to calculate age as of
     * @return age in years
     */
    public int calculateAge(LocalDate dateOfBirth, LocalDate asOfDate) {
        return (int) ChronoUnit.YEARS.between(dateOfBirth, asOfDate);
    }

    /**
     * Check if participant is eligible for catch-up contributions (age >= 50).
     * Age is determined as of December 31 of the tax year (IRS rule).
     * @param participant the participant
     * @param taxYear the tax year
     * @return true if age >= 50
     */
    public boolean isCatchUpEligible(Participant participant, int taxYear) {
        LocalDate endOfYear = LocalDate.of(taxYear, 12, 31);
        int age = calculateAge(participant.getDateOfBirth(), endOfYear);
        return age >= 50;
    }

    /**
     * Check if participant is eligible for super catch-up contributions (age 60-63 inclusive).
     * Age is determined as of December 31 of the tax year (IRS rule).
     * @param participant the participant
     * @param taxYear the tax year
     * @return true if age is 60-63
     */
    public boolean isSuperCatchUpEligible(Participant participant, int taxYear) {
        LocalDate endOfYear = LocalDate.of(taxYear, 12, 31);
        int age = calculateAge(participant.getDateOfBirth(), endOfYear);
        return age >= 60 && age <= 63;
    }

    /**
     * Check if participant must use Roth tax treatment for catch-up contributions.
     * Returns true if age >= 50 AND prior-year FICA wages exceed the threshold for that tax year.
     * @param participant the participant
     * @param taxYear the tax year
     * @return true if Roth catch-up is required
     */
    public boolean requiresRothCatchUp(Participant participant, int taxYear) {
        if (!isCatchUpEligible(participant, taxYear)) {
            return false;
        }

        Optional<ContributionLimit> limitOpt = contributionLimitRepository.findByTaxYear(taxYear);
        if (limitOpt.isEmpty()) {
            return false;
        }

        return participant.getPriorYearFicaWages() > limitOpt.get().getFicaWagesThreshold();
    }

    /**
     * Get the applicable catch-up limit for a participant in a given tax year.
     * Returns $11,250 for ages 60-63 (super catch-up), $7,500 otherwise.
     * @param participant the participant
     * @param taxYear the tax year
     * @return the applicable catch-up limit amount
     */
    public double getApplicableCatchUpLimit(Participant participant, int taxYear) {
        Optional<ContributionLimit> limitOpt = contributionLimitRepository.findByTaxYear(taxYear);
        if (limitOpt.isEmpty()) {
            return 0;
        }

        ContributionLimit limit = limitOpt.get();
        if (isSuperCatchUpEligible(participant, taxYear)) {
            return limit.getSuperCatchUpLimit();
        }
        return limit.getCatchUpLimit();
    }

    /**
     * Enforce tax treatment rules per SECURE 2.0 Section 603.
     * If the participant is catch-up eligible AND a high earner (FICA wages > threshold),
     * pre-tax catch-up contributions are rejected — must be Roth.
     * @param contribution the contribution to check
     * @throws ContributionValidationException if pre-tax catch-up is attempted by a high earner
     */
    public void enforceTaxTreatment(Contribution contribution) throws ContributionValidationException {
        if (contribution.getContributionType() == ContributionType.REGULAR) {
            return;
        }

        Participant participant = contribution.getParticipant();
        int taxYear = contribution.getPayrollDate().getYear();

        if (requiresRothCatchUp(participant, taxYear) && contribution.getTaxTreatment() == TaxTreatment.PRE_TAX) {
            throw new ContributionValidationException(
                    "SECURE 2.0 Section 603: Catch-up contributions for participants with prior-year FICA wages exceeding the threshold must be designated as Roth contributions.");
        }
    }
}

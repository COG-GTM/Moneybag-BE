package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.contribution.CreateContributionRequest;
import com.babakjan.moneybag.entity.Contribution;
import com.babakjan.moneybag.entity.Participant;
import com.babakjan.moneybag.entity.RetirementPlan;
import com.babakjan.moneybag.error.exception.ContributionValidationException;
import com.babakjan.moneybag.repository.ContributionRepository;
import com.babakjan.moneybag.repository.ParticipantRepository;
import com.babakjan.moneybag.repository.RetirementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;

    private final ParticipantRepository participantRepository;

    private final RetirementPlanRepository retirementPlanRepository;

    private final ContributionValidationService contributionValidationService;

    /**
     * Submit a new contribution. Validates against SECURE 2.0 rules before persisting.
     * @param request contribution data
     * @return saved contribution
     * @throws ContributionValidationException if the contribution violates SECURE 2.0 rules
     * @throws IllegalArgumentException if participant or plan not found
     */
    public Contribution save(CreateContributionRequest request) throws ContributionValidationException {
        Optional<Participant> participantOpt = participantRepository.findById(request.getParticipantId());
        if (participantOpt.isEmpty()) {
            throw new IllegalArgumentException("Participant of id: " + request.getParticipantId() + " not found.");
        }

        Optional<RetirementPlan> planOpt = retirementPlanRepository.findById(request.getPlanId());
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Retirement plan of id: " + request.getPlanId() + " not found.");
        }

        Contribution contribution = Contribution.builder()
                .participant(participantOpt.get())
                .plan(planOpt.get())
                .amount(request.getAmount())
                .contributionType(request.getContributionType())
                .taxTreatment(request.getTaxTreatment())
                .payrollDate(request.getPayrollDate())
                .build();

        contributionValidationService.validateContribution(contribution);

        return contributionRepository.save(contribution);
    }

    /**
     * Get contribution by id.
     * @param id contribution id
     * @return contribution
     * @throws IllegalArgumentException if not found
     */
    public Contribution getById(Long id) {
        Optional<Contribution> contributionOpt = contributionRepository.findById(id);
        if (contributionOpt.isEmpty()) {
            throw new IllegalArgumentException("Contribution of id: " + id + " not found.");
        }
        return contributionOpt.get();
    }

    /**
     * Get all contributions for a participant.
     * @param participantId participant id
     * @return list of contributions
     */
    public List<Contribution> getByParticipantId(Long participantId) {
        return contributionRepository.findByParticipantId(participantId);
    }
}

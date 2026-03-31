package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.plan.PlanConfigurationDto;
import com.babakjan.moneybag.dto.plan.UpdatePlanConfigRequest;
import com.babakjan.moneybag.entity.PlanConfiguration;
import com.babakjan.moneybag.entity.PlanConfigurationAuditLog;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.repository.PlanConfigurationAuditLogRepository;
import com.babakjan.moneybag.repository.PlanConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanConfigurationService {

    private final PlanConfigurationRepository planConfigurationRepository;

    private final PlanConfigurationAuditLogRepository auditLogRepository;

    /**
     * Get plan configuration by planId.
     * @param planId plan identifier
     * @return plan configuration
     * @throws PlanNotFoundException if plan not found
     */
    public PlanConfiguration getByPlanId(String planId) throws PlanNotFoundException {
        Optional<PlanConfiguration> optional = planConfigurationRepository.findByPlanId(planId);
        if (optional.isEmpty()) {
            throw new PlanNotFoundException(planId);
        }
        return optional.get();
    }

    /**
     * Create a new plan configuration.
     * @param dto plan configuration data
     * @return created plan configuration
     */
    public PlanConfiguration createPlanConfiguration(PlanConfigurationDto dto) {
        PlanConfiguration config = PlanConfiguration.builder()
                .planId(dto.getPlanId())
                .planName(dto.getPlanName())
                .planType(dto.getPlanType())
                .rothCatchUpAvailable(dto.getRothCatchUpAvailable())
                .rothEffectiveDate(dto.getRothEffectiveDate())
                .governmentalSpecialCatchUp(dto.getGovernmentalSpecialCatchUp())
                .build();
        return planConfigurationRepository.save(config);
    }

    /**
     * Update plan configuration with audit logging.
     * @param planId plan identifier
     * @param request update request with changes
     * @return updated plan configuration
     * @throws PlanNotFoundException if plan not found
     */
    public PlanConfiguration updatePlanConfiguration(String planId, UpdatePlanConfigRequest request)
            throws PlanNotFoundException {
        PlanConfiguration config = getByPlanId(planId);

        if (request.getRothCatchUpAvailable() != null
                && !request.getRothCatchUpAvailable().equals(config.getRothCatchUpAvailable())) {
            auditLogRepository.save(PlanConfigurationAuditLog.builder()
                    .planId(planId)
                    .fieldChanged("rothCatchUpAvailable")
                    .oldValue(String.valueOf(config.getRothCatchUpAvailable()))
                    .newValue(String.valueOf(request.getRothCatchUpAvailable()))
                    .changedBy(request.getChangedBy())
                    .reason(request.getReason())
                    .timestamp(new Date())
                    .build());
            config.setRothCatchUpAvailable(request.getRothCatchUpAvailable());
        }

        if (request.getRothEffectiveDate() != null
                && !request.getRothEffectiveDate().equals(config.getRothEffectiveDate())) {
            auditLogRepository.save(PlanConfigurationAuditLog.builder()
                    .planId(planId)
                    .fieldChanged("rothEffectiveDate")
                    .oldValue(String.valueOf(config.getRothEffectiveDate()))
                    .newValue(String.valueOf(request.getRothEffectiveDate()))
                    .changedBy(request.getChangedBy())
                    .reason(request.getReason())
                    .timestamp(new Date())
                    .build());
            config.setRothEffectiveDate(request.getRothEffectiveDate());
        }

        return planConfigurationRepository.save(config);
    }

    /**
     * Get audit log for a plan.
     * @param planId plan identifier
     * @return list of audit log entries
     */
    public List<PlanConfigurationAuditLog> getAuditLog(String planId) {
        return auditLogRepository.findByPlanIdOrderByTimestampDesc(planId);
    }

    /**
     * Check if Roth catch-up was available for a given plan at a specific contribution date.
     * @param planId plan identifier
     * @param contributionDate date of the contribution
     * @return true if Roth was available at the time of contribution
     */
    public boolean isRothAvailableForDate(String planId, Date contributionDate) {
        Optional<PlanConfiguration> optional = planConfigurationRepository.findByPlanId(planId);
        if (optional.isEmpty()) {
            return false;
        }
        PlanConfiguration config = optional.get();
        if (!Boolean.TRUE.equals(config.getRothCatchUpAvailable())) {
            return false;
        }
        if (config.getRothEffectiveDate() == null) {
            return true;
        }
        return !contributionDate.before(config.getRothEffectiveDate());
    }
}

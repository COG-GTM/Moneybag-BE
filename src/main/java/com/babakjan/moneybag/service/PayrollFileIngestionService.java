package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.payroll.*;
import com.babakjan.moneybag.entity.PayrollBatchSummary;
import com.babakjan.moneybag.entity.PayrollRecord;
import com.babakjan.moneybag.entity.PlanConfiguration;
import com.babakjan.moneybag.error.exception.PayrollProcessingException;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.repository.PayrollBatchSummaryRepository;
import com.babakjan.moneybag.repository.PayrollRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollFileIngestionService {

    private static final String ROTH_CATCHUP_REQUIRED = "ROTH_CATCHUP_REQUIRED";
    private static final String PLAN_NO_ROTH_OPTION = "PLAN_NO_ROTH_OPTION";
    private static final double GOVERNMENTAL_457B_SPECIAL_CATCHUP_LIMIT = 15000.0;

    private final PlanConfigurationService planConfigurationService;
    private final ParticipantEligibilityStub participantEligibilityStub;
    private final PayrollRecordRepository payrollRecordRepository;
    private final PayrollBatchSummaryRepository payrollBatchSummaryRepository;

    /**
     * Process a payroll file upload with SECURE 2.0 Section 603 validation.
     * @param request payroll file upload request
     * @return batch response with processing results
     * @throws PlanNotFoundException if plan configuration not found
     */
    public PayrollBatchResponse processPayrollFile(PayrollFileUploadRequest request) throws PlanNotFoundException {
        String batchId = UUID.randomUUID().toString();
        PlanConfiguration planConfig = planConfigurationService.getByPlanId(request.getPlanId());
        Date now = new Date();

        int processedCount = 0;
        int flaggedCount = 0;
        List<FlaggedRecordDetail> flaggedRecords = new ArrayList<>();

        for (PayrollRecordEntry entry : request.getRecords()) {
            PayrollRecord record = PayrollRecord.builder()
                    .batchId(batchId)
                    .participantId(entry.getParticipantId())
                    .participantName(entry.getParticipantName())
                    .amount(entry.getAmount())
                    .contributionType(entry.getContributionType())
                    .designation(entry.getDesignation())
                    .submittedAt(now)
                    .build();

            if ("REGULAR".equals(entry.getContributionType())) {
                record.setStatus("PROCESSED");
                record.setProcessedAt(now);
                processedCount++;
            } else if ("CATCHUP".equals(entry.getContributionType())
                    || "SUPER_CATCHUP".equals(entry.getContributionType())) {

                ProcessingResult result = processCatchUpContribution(
                        entry, record, planConfig, now);
                if (result.isFlagged()) {
                    flaggedCount++;
                    flaggedRecords.add(result.getFlaggedDetail());
                } else {
                    processedCount++;
                }
            } else {
                record.setStatus("PROCESSED");
                record.setProcessedAt(now);
                processedCount++;
            }

            payrollRecordRepository.save(record);
        }

        PayrollBatchSummary summary = PayrollBatchSummary.builder()
                .batchId(batchId)
                .planId(request.getPlanId())
                .totalRecords(request.getRecords().size())
                .processedCount(processedCount)
                .flaggedCount(flaggedCount)
                .submittedAt(now)
                .completedAt(flaggedCount == 0 ? now : null)
                .build();
        payrollBatchSummaryRepository.save(summary);

        String guidance = flaggedCount > 0
                ? "Flagged records must be resubmitted with Roth designation. Records are held in pending queue."
                : null;

        return PayrollBatchResponse.builder()
                .batchId(batchId)
                .totalRecords(request.getRecords().size())
                .processedCount(processedCount)
                .flaggedCount(flaggedCount)
                .flaggedRecords(flaggedRecords)
                .remediationGuidance(guidance)
                .build();
    }

    private ProcessingResult processCatchUpContribution(
            PayrollRecordEntry entry,
            PayrollRecord record,
            PlanConfiguration planConfig,
            Date now) {

        boolean isHighEarner = participantEligibilityStub.isHighEarner(entry.getParticipantId());

        if (!isHighEarner) {
            record.setStatus("PROCESSED");
            record.setProcessedAt(now);
            return new ProcessingResult(false, null);
        }

        // Governmental 457(b) special catch-up exemption
        if ("GOVERNMENTAL_457B".equals(planConfig.getPlanType())
                && Boolean.TRUE.equals(planConfig.getGovernmentalSpecialCatchUp())) {
            return processGovernmental457bCatchUp(entry, record, planConfig, now);
        }

        // Check if plan has Roth option
        if (!Boolean.TRUE.equals(planConfig.getRothCatchUpAvailable())) {
            record.setStatus("REJECTED");
            record.setErrorCode(PLAN_NO_ROTH_OPTION);
            record.setErrorMessage("Plan does not offer Roth catch-up option. Catch-up contributions from affected participants cannot be processed.");
            return new ProcessingResult(true, buildFlaggedDetail(entry, PLAN_NO_ROTH_OPTION,
                    "Plan does not offer Roth catch-up option. Catch-up contributions from affected participants cannot be processed."));
        }

        // Check if Roth effective date has been reached
        if (planConfig.getRothEffectiveDate() != null && now.before(planConfig.getRothEffectiveDate())) {
            record.setStatus("REJECTED");
            record.setErrorCode(PLAN_NO_ROTH_OPTION);
            record.setErrorMessage("Roth catch-up option not yet effective for this plan.");
            return new ProcessingResult(true, buildFlaggedDetail(entry, PLAN_NO_ROTH_OPTION,
                    "Roth catch-up option not yet effective for this plan."));
        }

        // High earner with pre-tax designation: flag
        if ("PRE_TAX".equals(entry.getDesignation())) {
            record.setStatus("FLAGGED");
            record.setErrorCode(ROTH_CATCHUP_REQUIRED);
            record.setErrorMessage("High earner catch-up contributions must be designated as Roth under SECURE 2.0 Section 603.");
            return new ProcessingResult(true, buildFlaggedDetail(entry, ROTH_CATCHUP_REQUIRED,
                    "High earner catch-up contributions must be designated as Roth under SECURE 2.0 Section 603."));
        }

        // High earner with Roth designation: process normally
        record.setStatus("PROCESSED");
        record.setProcessedAt(now);
        return new ProcessingResult(false, null);
    }

    private ProcessingResult processGovernmental457bCatchUp(
            PayrollRecordEntry entry,
            PayrollRecord record,
            PlanConfiguration planConfig,
            Date now) {

        double amount = entry.getAmount();

        // Special catch-up amounts are fully exempt from Roth requirement
        if (amount <= GOVERNMENTAL_457B_SPECIAL_CATCHUP_LIMIT) {
            record.setStatus("PROCESSED");
            record.setProcessedAt(now);
            return new ProcessingResult(false, null);
        }

        // Amount exceeds special catch-up limit — the excess is subject to Roth enforcement
        double excessAmount = amount - GOVERNMENTAL_457B_SPECIAL_CATCHUP_LIMIT;

        if ("PRE_TAX".equals(entry.getDesignation())) {
            // Check if plan has Roth option for the excess
            if (!Boolean.TRUE.equals(planConfig.getRothCatchUpAvailable())) {
                record.setStatus("REJECTED");
                record.setErrorCode(PLAN_NO_ROTH_OPTION);
                record.setErrorMessage("Excess amount $" + String.format("%.2f", excessAmount)
                        + " above special catch-up limit requires Roth, but plan has no Roth option.");
                return new ProcessingResult(true, buildFlaggedDetail(entry, PLAN_NO_ROTH_OPTION,
                        "Excess amount $" + String.format("%.2f", excessAmount)
                                + " above special catch-up limit requires Roth, but plan has no Roth option."));
            }

            // Check if Roth effective date has been reached
            if (planConfig.getRothEffectiveDate() != null && now.before(planConfig.getRothEffectiveDate())) {
                record.setStatus("REJECTED");
                record.setErrorCode(PLAN_NO_ROTH_OPTION);
                record.setErrorMessage("Roth catch-up option not yet effective for this plan.");
                return new ProcessingResult(true, buildFlaggedDetail(entry, PLAN_NO_ROTH_OPTION,
                        "Roth catch-up option not yet effective for this plan."));
            }

            record.setStatus("FLAGGED");
            record.setErrorCode(ROTH_CATCHUP_REQUIRED);
            record.setErrorMessage("$" + String.format("%.2f", excessAmount)
                    + " exceeds governmental 457(b) special catch-up limit and must be designated as Roth.");
            return new ProcessingResult(true, buildFlaggedDetail(entry, ROTH_CATCHUP_REQUIRED,
                    "$" + String.format("%.2f", excessAmount)
                            + " exceeds governmental 457(b) special catch-up limit and must be designated as Roth."));
        }

        // Roth designation: process normally
        record.setStatus("PROCESSED");
        record.setProcessedAt(now);
        return new ProcessingResult(false, null);
    }

    /**
     * Resubmit a flagged payroll record with a new designation.
     * @param request resubmission request
     * @return batch response with updated status
     * @throws PayrollProcessingException if record not found or still invalid
     */
    public PayrollBatchResponse resubmitFlaggedRecord(PayrollResubmissionRequest request)
            throws PayrollProcessingException {
        Optional<PayrollRecord> optional = payrollRecordRepository
                .findByBatchIdAndParticipantId(request.getBatchId(), request.getParticipantId());

        if (optional.isEmpty()) {
            throw new PayrollProcessingException("RECORD_NOT_FOUND",
                    "No record found for batchId: " + request.getBatchId()
                            + " and participantId: " + request.getParticipantId());
        }

        PayrollRecord record = optional.get();

        if (!"FLAGGED".equals(record.getStatus()) && !"PENDING_RESUBMISSION".equals(record.getStatus())) {
            throw new PayrollProcessingException("INVALID_STATUS",
                    "Record is not in a flagged or pending state. Current status: " + record.getStatus());
        }

        if ("ROTH".equals(request.getNewDesignation())) {
            record.setDesignation("ROTH");
            record.setStatus("PROCESSED");
            record.setProcessedAt(new Date());
            record.setErrorCode(null);
            record.setErrorMessage(null);
            payrollRecordRepository.save(record);

            updateBatchSummaryAfterResubmission(request.getBatchId());
        } else {
            record.setStatus("FLAGGED");
            payrollRecordRepository.save(record);

            throw new PayrollProcessingException(record.getErrorCode(),
                    "Record still requires Roth designation. Pre-tax catch-up not allowed for high earners under SECURE 2.0 Section 603.");
        }

        return getBatchResponse(request.getBatchId());
    }

    private void updateBatchSummaryAfterResubmission(String batchId) {
        Optional<PayrollBatchSummary> optional = payrollBatchSummaryRepository.findByBatchId(batchId);
        if (optional.isPresent()) {
            PayrollBatchSummary summary = optional.get();
            List<PayrollRecord> allRecords = payrollRecordRepository.findByBatchId(batchId);
            int processed = 0;
            int flagged = 0;
            for (PayrollRecord r : allRecords) {
                if ("PROCESSED".equals(r.getStatus())) {
                    processed++;
                } else if ("FLAGGED".equals(r.getStatus()) || "PENDING_RESUBMISSION".equals(r.getStatus())
                        || "REJECTED".equals(r.getStatus())) {
                    flagged++;
                }
            }
            summary.setProcessedCount(processed);
            summary.setFlaggedCount(flagged);
            if (flagged == 0) {
                summary.setCompletedAt(new Date());
            }
            payrollBatchSummaryRepository.save(summary);
        }
    }

    /**
     * Get batch response by batchId.
     * @param batchId batch identifier
     * @return batch response
     * @throws PayrollProcessingException if batch not found
     */
    public PayrollBatchResponse getBatchResponse(String batchId) throws PayrollProcessingException {
        Optional<PayrollBatchSummary> optional = payrollBatchSummaryRepository.findByBatchId(batchId);
        if (optional.isEmpty()) {
            throw new PayrollProcessingException("BATCH_NOT_FOUND",
                    "Batch with id: " + batchId + " not found.");
        }

        PayrollBatchSummary summary = optional.get();
        List<PayrollRecord> flaggedRecords = payrollRecordRepository
                .findByBatchIdAndStatus(batchId, "FLAGGED");

        List<FlaggedRecordDetail> flaggedDetails = flaggedRecords.stream()
                .map(r -> FlaggedRecordDetail.builder()
                        .participantId(r.getParticipantId())
                        .participantName(r.getParticipantName())
                        .amount(r.getAmount())
                        .errorCode(r.getErrorCode())
                        .errorMessage(r.getErrorMessage())
                        .build())
                .collect(Collectors.toList());

        String guidance = summary.getFlaggedCount() > 0
                ? "Flagged records must be resubmitted with Roth designation. Records are held in pending queue."
                : null;

        return PayrollBatchResponse.builder()
                .batchId(summary.getBatchId())
                .totalRecords(summary.getTotalRecords())
                .processedCount(summary.getProcessedCount())
                .flaggedCount(summary.getFlaggedCount())
                .flaggedRecords(flaggedDetails)
                .remediationGuidance(guidance)
                .build();
    }

    /**
     * Get flagged records for a batch.
     * @param batchId batch identifier
     * @return list of flagged record details
     */
    public List<FlaggedRecordDetail> getFlaggedRecords(String batchId) {
        List<PayrollRecord> flaggedRecords = payrollRecordRepository
                .findByBatchIdAndStatus(batchId, "FLAGGED");

        return flaggedRecords.stream()
                .map(r -> FlaggedRecordDetail.builder()
                        .participantId(r.getParticipantId())
                        .participantName(r.getParticipantName())
                        .amount(r.getAmount())
                        .errorCode(r.getErrorCode())
                        .errorMessage(r.getErrorMessage())
                        .build())
                .collect(Collectors.toList());
    }

    private FlaggedRecordDetail buildFlaggedDetail(PayrollRecordEntry entry, String errorCode, String errorMessage) {
        return FlaggedRecordDetail.builder()
                .participantId(entry.getParticipantId())
                .participantName(entry.getParticipantName())
                .amount(entry.getAmount())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Simple holder for processing results.
     */
    private static class ProcessingResult {
        private final boolean flagged;
        private final FlaggedRecordDetail flaggedDetail;

        ProcessingResult(boolean flagged, FlaggedRecordDetail flaggedDetail) {
            this.flagged = flagged;
            this.flaggedDetail = flaggedDetail;
        }

        boolean isFlagged() {
            return flagged;
        }

        FlaggedRecordDetail getFlaggedDetail() {
            return flaggedDetail;
        }
    }
}

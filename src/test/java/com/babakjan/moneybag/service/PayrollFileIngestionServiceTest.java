package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.payroll.*;
import com.babakjan.moneybag.entity.PayrollBatchSummary;
import com.babakjan.moneybag.entity.PayrollRecord;
import com.babakjan.moneybag.entity.PlanConfiguration;
import com.babakjan.moneybag.error.exception.PayrollProcessingException;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.repository.PayrollBatchSummaryRepository;
import com.babakjan.moneybag.repository.PayrollRecordRepository;
import com.babakjan.moneybag.repository.PlanConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PayrollFileIngestionServiceTest {

    @Autowired
    private PayrollFileIngestionService payrollFileIngestionService;

    @Autowired
    private PlanConfigurationService planConfigurationService;

    @Autowired
    private ParticipantEligibilityStub participantEligibilityStub;

    @Autowired
    private PlanConfigurationRepository planConfigurationRepository;

    @Autowired
    private PayrollRecordRepository payrollRecordRepository;

    @Autowired
    private PayrollBatchSummaryRepository payrollBatchSummaryRepository;

    @BeforeEach
    void setUp() {
        payrollRecordRepository.deleteAll();
        payrollBatchSummaryRepository.deleteAll();
        planConfigurationRepository.deleteAll();
        participantEligibilityStub.clear();
    }

    private PlanConfiguration createDefaultPlan(String planId, boolean rothAvailable) {
        return planConfigurationRepository.save(PlanConfiguration.builder()
                .planId(planId)
                .planName("Test Plan")
                .planType("401K")
                .rothCatchUpAvailable(rothAvailable)
                .rothEffectiveDate(null)
                .governmentalSpecialCatchUp(false)
                .build());
    }

    // Test 1: 500 participants, 12 have pre-tax catch-up + high earner => 488 processed, 12 flagged
    @Test
    void testScenario1_500Participants_12PreTaxCatchUpHighEarners() throws PlanNotFoundException {
        createDefaultPlan("PLAN-001", true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        // 488 regular participants
        for (long i = 1; i <= 488; i++) {
            records.add(PayrollRecordEntry.builder()
                    .participantId(i)
                    .participantName("Participant " + i)
                    .amount(500.0)
                    .contributionType("REGULAR")
                    .designation("PRE_TAX")
                    .build());
        }
        // 12 high earners with pre-tax catch-up
        for (long i = 489; i <= 500; i++) {
            participantEligibilityStub.setHighEarner(i, true);
            records.add(PayrollRecordEntry.builder()
                    .participantId(i)
                    .participantName("High Earner " + i)
                    .amount(7500.0)
                    .contributionType("CATCHUP")
                    .designation("PRE_TAX")
                    .build());
        }

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-001")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(500, response.getTotalRecords());
        assertEquals(488, response.getProcessedCount());
        assertEquals(12, response.getFlaggedCount());
        assertEquals(12, response.getFlaggedRecords().size());
        assertNotNull(response.getRemediationGuidance());

        for (FlaggedRecordDetail detail : response.getFlaggedRecords()) {
            assertEquals("ROTH_CATCHUP_REQUIRED", detail.getErrorCode());
        }
    }

    // Test 2: 500 participants, 12 have Roth catch-up + high earner => all 500 processed
    @Test
    void testScenario2_500Participants_12RothCatchUpHighEarners() throws PlanNotFoundException {
        createDefaultPlan("PLAN-002", true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        for (long i = 1; i <= 488; i++) {
            records.add(PayrollRecordEntry.builder()
                    .participantId(i)
                    .participantName("Participant " + i)
                    .amount(500.0)
                    .contributionType("REGULAR")
                    .designation("PRE_TAX")
                    .build());
        }
        for (long i = 489; i <= 500; i++) {
            participantEligibilityStub.setHighEarner(i, true);
            records.add(PayrollRecordEntry.builder()
                    .participantId(i)
                    .participantName("High Earner " + i)
                    .amount(7500.0)
                    .contributionType("CATCHUP")
                    .designation("ROTH")
                    .build());
        }

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-002")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(500, response.getTotalRecords());
        assertEquals(500, response.getProcessedCount());
        assertEquals(0, response.getFlaggedCount());
        assertTrue(response.getFlaggedRecords().isEmpty());
    }

    // Test 3: All records are regular (non-catch-up) => all processed, Section 603 skipped
    @Test
    void testScenario3_AllRegularContributions() throws PlanNotFoundException {
        createDefaultPlan("PLAN-003", true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            // Even high earners with regular contributions should process normally
            participantEligibilityStub.setHighEarner(i, true);
            records.add(PayrollRecordEntry.builder()
                    .participantId(i)
                    .participantName("Participant " + i)
                    .amount(1000.0)
                    .contributionType("REGULAR")
                    .designation("PRE_TAX")
                    .build());
        }

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-003")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(100, response.getTotalRecords());
        assertEquals(100, response.getProcessedCount());
        assertEquals(0, response.getFlaggedCount());
    }

    // Test 4: Re-submitted flagged record now designated Roth => accepted
    @Test
    void testScenario4_ResubmitFlaggedRecordAsRoth() throws PlanNotFoundException, PayrollProcessingException {
        createDefaultPlan("PLAN-004", true);
        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest uploadRequest = PayrollFileUploadRequest.builder()
                .planId("PLAN-004")
                .records(records)
                .build();

        PayrollBatchResponse initialResponse = payrollFileIngestionService.processPayrollFile(uploadRequest);
        assertEquals(1, initialResponse.getFlaggedCount());

        PayrollResubmissionRequest resubmitRequest = PayrollResubmissionRequest.builder()
                .batchId(initialResponse.getBatchId())
                .participantId(1L)
                .newDesignation("ROTH")
                .build();

        PayrollBatchResponse resubmitResponse = payrollFileIngestionService.resubmitFlaggedRecord(resubmitRequest);

        assertEquals(1, resubmitResponse.getProcessedCount());
        assertEquals(0, resubmitResponse.getFlaggedCount());
    }

    // Test 5: Flagged record re-submitted still as pre-tax => rejected again
    @Test
    void testScenario5_ResubmitFlaggedRecordStillPreTax() throws PlanNotFoundException {
        createDefaultPlan("PLAN-005", true);
        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest uploadRequest = PayrollFileUploadRequest.builder()
                .planId("PLAN-005")
                .records(records)
                .build();

        PayrollBatchResponse initialResponse = payrollFileIngestionService.processPayrollFile(uploadRequest);
        assertEquals(1, initialResponse.getFlaggedCount());

        PayrollResubmissionRequest resubmitRequest = PayrollResubmissionRequest.builder()
                .batchId(initialResponse.getBatchId())
                .participantId(1L)
                .newDesignation("PRE_TAX")
                .build();

        PayrollProcessingException exception = assertThrows(
                PayrollProcessingException.class,
                () -> payrollFileIngestionService.resubmitFlaggedRecord(resubmitRequest));

        assertEquals("ROTH_CATCHUP_REQUIRED", exception.getErrorCode());
    }

    // Test 6: Plan has no Roth option, affected participant submits catch-up => rejected with PLAN_NO_ROTH_OPTION
    @Test
    void testScenario6_PlanNoRothOption() throws PlanNotFoundException {
        createDefaultPlan("PLAN-006", false);
        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-006")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getProcessedCount());
        assertEquals(1, response.getFlaggedCount());
        assertEquals("PLAN_NO_ROTH_OPTION", response.getFlaggedRecords().get(0).getErrorCode());
    }

    // Test 7: Plan enables Roth mid-year (effective 4/1), catch-up submitted 4/15 => accepted as Roth
    @Test
    void testScenario7_MidYearRothEnable_AfterEffectiveDate() throws PlanNotFoundException {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date effectiveDate = cal.getTime();

        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("PLAN-007")
                .planName("Mid-Year Plan")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(effectiveDate)
                .governmentalSpecialCatchUp(false)
                .build());

        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("ROTH")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-007")
                .records(records)
                .build();

        // Current date is after 4/1/2025, so Roth should be available
        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getProcessedCount());
        assertEquals(0, response.getFlaggedCount());
    }

    // Test 8: Plan enables Roth mid-year (effective date in far future), catch-up submitted now => rejected
    @Test
    void testScenario8_MidYearRothEnable_BeforeEffectiveDate() throws PlanNotFoundException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5); // Set effective date 5 years in the future
        Date futureEffectiveDate = cal.getTime();

        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("PLAN-008")
                .planName("Future Roth Plan")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(futureEffectiveDate)
                .governmentalSpecialCatchUp(false)
                .build());

        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-008")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getProcessedCount());
        assertEquals(1, response.getFlaggedCount());
        assertEquals("PLAN_NO_ROTH_OPTION", response.getFlaggedRecords().get(0).getErrorCode());
    }

    // Test 9: Governmental 457(b), special catch-up of $15,000 => exempt from Roth requirement
    @Test
    void testScenario9_Governmental457b_SpecialCatchUpExempt() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("PLAN-009")
                .planName("Gov 457(b) Plan")
                .planType("GOVERNMENTAL_457B")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(null)
                .governmentalSpecialCatchUp(true)
                .build());

        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("Gov Employee 1")
                .amount(15000.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-009")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getProcessedCount());
        assertEquals(0, response.getFlaggedCount());
    }

    // Test 10: Governmental 457(b), catch-up of $20,000 ($15K special + $5K standard)
    // $15K exempt, $5K subject to Roth if affected
    @Test
    void testScenario10_Governmental457b_ExcessSubjectToRoth() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("PLAN-010")
                .planName("Gov 457(b) Plan")
                .planType("GOVERNMENTAL_457B")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(null)
                .governmentalSpecialCatchUp(true)
                .build());

        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("Gov Employee 1")
                .amount(20000.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-010")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getProcessedCount());
        assertEquals(1, response.getFlaggedCount());
        assertEquals("ROTH_CATCHUP_REQUIRED", response.getFlaggedRecords().get(0).getErrorCode());
        assertTrue(response.getFlaggedRecords().get(0).getErrorMessage().contains("5000.00"));
    }

    // Test: Non-high earner with catch-up contribution processes normally regardless of designation
    @Test
    void testNonHighEarner_CatchUpProcessesNormally() throws PlanNotFoundException {
        createDefaultPlan("PLAN-011", true);
        participantEligibilityStub.setHighEarner(1L, false);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("Regular Earner")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest request = PayrollFileUploadRequest.builder()
                .planId("PLAN-011")
                .records(records)
                .build();

        PayrollBatchResponse response = payrollFileIngestionService.processPayrollFile(request);

        assertEquals(1, response.getProcessedCount());
        assertEquals(0, response.getFlaggedCount());
    }

    // Test: Get batch response
    @Test
    void testGetBatchResponse() throws PlanNotFoundException, PayrollProcessingException {
        createDefaultPlan("PLAN-012", true);
        participantEligibilityStub.setHighEarner(1L, true);

        List<PayrollRecordEntry> records = new ArrayList<>();
        records.add(PayrollRecordEntry.builder()
                .participantId(1L)
                .participantName("High Earner 1")
                .amount(7500.0)
                .contributionType("CATCHUP")
                .designation("PRE_TAX")
                .build());
        records.add(PayrollRecordEntry.builder()
                .participantId(2L)
                .participantName("Regular 2")
                .amount(500.0)
                .contributionType("REGULAR")
                .designation("PRE_TAX")
                .build());

        PayrollFileUploadRequest uploadRequest = PayrollFileUploadRequest.builder()
                .planId("PLAN-012")
                .records(records)
                .build();

        PayrollBatchResponse initialResponse = payrollFileIngestionService.processPayrollFile(uploadRequest);

        PayrollBatchResponse fetchedResponse = payrollFileIngestionService.getBatchResponse(initialResponse.getBatchId());

        assertEquals(initialResponse.getBatchId(), fetchedResponse.getBatchId());
        assertEquals(2, fetchedResponse.getTotalRecords());
        assertEquals(1, fetchedResponse.getProcessedCount());
        assertEquals(1, fetchedResponse.getFlaggedCount());
    }

    // Test: Batch not found throws exception
    @Test
    void testGetBatchResponse_NotFound() {
        PayrollProcessingException exception = assertThrows(
                PayrollProcessingException.class,
                () -> payrollFileIngestionService.getBatchResponse("non-existent-batch"));

        assertEquals("BATCH_NOT_FOUND", exception.getErrorCode());
    }
}

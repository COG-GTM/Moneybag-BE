package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.payroll.*;
import com.babakjan.moneybag.error.exception.PayrollProcessingException;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.service.PayrollFileIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/payroll", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Payroll file processing for SECURE 2.0 Section 603 compliance")
public class PayrollController {

    private final PayrollFileIngestionService payrollFileIngestionService;

    @PostMapping("/process")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Process a payroll file upload with SECURE 2.0 Section 603 validation.")
    public PayrollBatchResponse processPayrollFile(@RequestBody PayrollFileUploadRequest request)
            throws PlanNotFoundException {
        return payrollFileIngestionService.processPayrollFile(request);
    }

    @PostMapping("/resubmit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Resubmit a flagged payroll record with a new designation.")
    public PayrollBatchResponse resubmitFlaggedRecord(@RequestBody PayrollResubmissionRequest request)
            throws PayrollProcessingException {
        return payrollFileIngestionService.resubmitFlaggedRecord(request);
    }

    @GetMapping("/batch/{batchId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get batch processing results by batchId.")
    public PayrollBatchResponse getBatch(@PathVariable String batchId)
            throws PayrollProcessingException {
        return payrollFileIngestionService.getBatchResponse(batchId);
    }

    @GetMapping("/batch/{batchId}/flagged")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get flagged records for a batch.")
    public List<FlaggedRecordDetail> getFlaggedRecords(@PathVariable String batchId) {
        return payrollFileIngestionService.getFlaggedRecords(batchId);
    }
}

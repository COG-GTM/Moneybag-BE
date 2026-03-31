package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.tax.TaxReportResponse;
import com.babakjan.moneybag.dto.tax.YearEndTaxSummaryResponse;
import com.babakjan.moneybag.error.exception.TaxReportingException;
import com.babakjan.moneybag.service.TaxReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/tax", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Tax Reporting", description = "SECURE 2.0 Section 603 tax reporting")
public class TaxReportingController {

    private final TaxReportingService taxReportingService;

    @GetMapping("/{participantId}/report")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Generate tax report",
            description = "Generates a tax report with W-2 Box 12 codes and 1099-R distribution codes")
    public TaxReportResponse getTaxReport(
            @PathVariable Long participantId,
            @RequestParam int planYear) throws TaxReportingException {
        return taxReportingService.generateTaxReport(participantId, planYear);
    }

    @GetMapping("/{participantId}/year-end-summary")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Generate year-end tax summary",
            description = "Generates a comprehensive year-end tax summary with all contribution types")
    public YearEndTaxSummaryResponse getYearEndSummary(
            @PathVariable Long participantId,
            @RequestParam int planYear) throws TaxReportingException {
        return taxReportingService.generateYearEndSummary(participantId, planYear);
    }
}

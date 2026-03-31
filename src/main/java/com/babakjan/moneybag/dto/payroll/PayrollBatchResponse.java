package com.babakjan.moneybag.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollBatchResponse {
    private String batchId;
    private int totalRecords;
    private int processedCount;
    private int flaggedCount;
    private List<FlaggedRecordDetail> flaggedRecords;
    private String remediationGuidance;
}

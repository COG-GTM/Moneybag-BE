package com.babakjan.moneybag.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollResubmissionRequest {
    private String batchId;
    private Long participantId;
    private String newDesignation;
}

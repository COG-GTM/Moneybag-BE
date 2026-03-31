package com.babakjan.moneybag.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollRecordEntry {
    private Long participantId;
    private String participantName;
    private Double amount;
    private String contributionType;
    private String designation;
}

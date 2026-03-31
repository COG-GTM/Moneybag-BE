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
public class PayrollFileUploadRequest {
    private String planId;
    private List<PayrollRecordEntry> records;
}

package com.babakjan.moneybag.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlaggedRecordDetail {
    private Long participantId;
    private String participantName;
    private Double amount;
    private String errorCode;
    private String errorMessage;
}

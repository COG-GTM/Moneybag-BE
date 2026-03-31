package com.babakjan.moneybag.dto.participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ElectionSubmissionResponse {
    private boolean success;
    private Long electionId;
    private String catchUpLabel;
    private String projectedAnnualTaxImpact;
    private String errorCode;
    private String errorMessage;
}

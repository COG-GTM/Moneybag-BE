package com.babakjan.moneybag.dto.contribution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionResponse {
    private boolean accepted;
    private Long contributionId;
    private String errorCode;
    private String errorMessage;
}

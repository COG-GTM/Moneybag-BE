package com.babakjan.moneybag.dto.contribution;

import com.babakjan.moneybag.entity.ContributionDesignation;
import com.babakjan.moneybag.entity.ContributionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionRequest {
    private Long participantId;
    private Double amount;
    private ContributionType contributionType;
    private ContributionDesignation designation;
    private int planYear;
}

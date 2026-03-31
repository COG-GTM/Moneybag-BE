package com.babakjan.moneybag.dto.contribution;

import com.babakjan.moneybag.entity.ContributionType;
import com.babakjan.moneybag.entity.TaxTreatment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionDto {

    private Long id;

    private Long participantId;

    private Long planId;

    private Double amount;

    private ContributionType contributionType;

    private TaxTreatment taxTreatment;

    private LocalDate payrollDate;
}

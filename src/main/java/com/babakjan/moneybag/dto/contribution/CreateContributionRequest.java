package com.babakjan.moneybag.dto.contribution;

import com.babakjan.moneybag.entity.ContributionType;
import com.babakjan.moneybag.entity.TaxTreatment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateContributionRequest {

    @NotNull
    private Long participantId;

    @NotNull
    private Long planId;

    @NotNull
    private Double amount;

    @NotNull
    private ContributionType contributionType;

    @NotNull
    private TaxTreatment taxTreatment;

    @NotNull
    private LocalDate payrollDate;
}

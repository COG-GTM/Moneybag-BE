package com.babakjan.moneybag.dto.contribution;

import com.babakjan.moneybag.entity.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RetirementPlanDto {

    private Long id;

    private PlanType planType;

    private String planName;

    private String employerName;
}

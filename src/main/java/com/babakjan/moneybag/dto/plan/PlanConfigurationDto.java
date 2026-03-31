package com.babakjan.moneybag.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanConfigurationDto {
    private String planId;
    private String planName;
    private String planType;
    private Boolean rothCatchUpAvailable;
    private Date rothEffectiveDate;
    private Boolean governmentalSpecialCatchUp;
}

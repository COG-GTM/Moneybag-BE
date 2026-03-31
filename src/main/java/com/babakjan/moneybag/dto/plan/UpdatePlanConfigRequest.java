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
public class UpdatePlanConfigRequest {
    private Boolean rothCatchUpAvailable;
    private Date rothEffectiveDate;
    private String changedBy;
    private String reason;
}

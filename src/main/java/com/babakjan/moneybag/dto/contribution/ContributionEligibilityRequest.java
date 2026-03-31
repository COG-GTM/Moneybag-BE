package com.babakjan.moneybag.dto.contribution;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContributionEligibilityRequest {
    @NotNull
    private Long participantId;

    @NotNull
    private Integer planYear;
}

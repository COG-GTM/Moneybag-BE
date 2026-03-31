package com.babakjan.moneybag.dto.participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ElectionSubmissionRequest {
    private Long participantId;
    private int planYear;
    private Double regularAmount;
    private String regularDesignation;
    private Double catchUpAmount;
    private String catchUpDesignation;
}

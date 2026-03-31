package com.babakjan.moneybag.dto.participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantElectionOptionsResponse {
    private Long participantId;
    private int planYear;
    private int age;
    private boolean affectedBySection603;
    private boolean catchUpEligible;
    private boolean superCatchUpEligible;
    private Double catchUpLimit;
    private Double regularContributionLimit;
    private boolean preTaxCatchUpAvailable;
    private boolean rothCatchUpAvailable;
    private boolean planHasRothOption;
    private String section603Message;
    private String planNoRothMessage;
    private String learnMoreUrl;
}

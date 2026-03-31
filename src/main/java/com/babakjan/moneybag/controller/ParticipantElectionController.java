package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.participant.ContributionSummaryResponse;
import com.babakjan.moneybag.dto.participant.ElectionSubmissionRequest;
import com.babakjan.moneybag.dto.participant.ElectionSubmissionResponse;
import com.babakjan.moneybag.dto.participant.ParticipantElectionOptionsResponse;
import com.babakjan.moneybag.error.exception.ElectionValidationException;
import com.babakjan.moneybag.service.ContributionSummaryService;
import com.babakjan.moneybag.service.ParticipantElectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/participant", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Participant Election", description = "SECURE 2.0 Section 603 participant election workflows")
public class ParticipantElectionController {

    private final ParticipantElectionService participantElectionService;
    private final ContributionSummaryService contributionSummaryService;

    @GetMapping("/{participantId}/election-options")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get election options for a participant",
            description = "Returns available contribution election options based on Section 603 eligibility")
    public ParticipantElectionOptionsResponse getElectionOptions(
            @PathVariable Long participantId,
            @RequestParam int planYear) {
        return participantElectionService.getElectionOptions(participantId, planYear);
    }

    @PostMapping("/{participantId}/election")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a contribution election",
            description = "Submits a new contribution election with Section 603 validation")
    public ElectionSubmissionResponse submitElection(
            @PathVariable Long participantId,
            @RequestBody ElectionSubmissionRequest request) throws ElectionValidationException {
        request.setParticipantId(participantId);
        return participantElectionService.submitElection(request);
    }

    @GetMapping("/{participantId}/contribution-summary")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get contribution summary",
            description = "Returns current contribution summary for a participant and plan year")
    public ContributionSummaryResponse getContributionSummary(
            @PathVariable Long participantId,
            @RequestParam int planYear) {
        return contributionSummaryService.getSummary(participantId, planYear);
    }
}

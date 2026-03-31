package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.contribution.ContributionEligibilityResponse;
import com.babakjan.moneybag.dto.contribution.ContributionRequest;
import com.babakjan.moneybag.dto.contribution.ContributionResponse;
import com.babakjan.moneybag.error.exception.ContributionRoutingException;
import com.babakjan.moneybag.service.ContributionEligibilityService;
import com.babakjan.moneybag.service.ContributionRoutingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/contributions", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Contribution", description = "SECURE 2.0 Section 603 Contribution Routing")
public class ContributionController {

    private final ContributionEligibilityService eligibilityService;
    private final ContributionRoutingEngine routingEngine;

    @PostMapping("/eligibility")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Determine catch-up contribution eligibility",
            description = "Evaluates a participant's eligibility for catch-up contributions per SECURE 2.0 Section 603"
    )
    public ContributionEligibilityResponse checkEligibility(@RequestBody Map<String, Object> request)
            throws ContributionRoutingException {
        Long participantId = ((Number) request.get("participantId")).longValue();
        int planYear = ((Number) request.get("planYear")).intValue();
        return eligibilityService.determineEligibility(participantId, planYear);
    }

    @PostMapping("/route")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Route a contribution",
            description = "Routes a contribution request based on SECURE 2.0 Section 603 eligibility rules"
    )
    public ContributionResponse routeContribution(@RequestBody ContributionRequest request)
            throws ContributionRoutingException {
        return routingEngine.routeContribution(request);
    }
}

package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.contribution.ContributionDto;
import com.babakjan.moneybag.dto.contribution.CreateContributionRequest;
import com.babakjan.moneybag.error.exception.ContributionValidationException;
import com.babakjan.moneybag.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/contributions", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Contribution", description = "Retirement plan contributions")
@SecurityRequirement(name = "bearer-key")
public class ContributionController {

    private final ContributionService contributionService;

    /**
     * Submit a new contribution. Validates against SECURE 2.0 rules before persisting.
     * @param request contribution data
     * @return created contribution
     * @throws ContributionValidationException if the contribution violates SECURE 2.0 rules
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit a new contribution.",
            description = "Validates against SECURE 2.0 Section 603 rules before persisting."
    )
    public ContributionDto createContribution(@RequestBody @Valid CreateContributionRequest request)
            throws ContributionValidationException {
        return contributionService.save(request).dto();
    }

    /**
     * Get contribution by id.
     * @param id contribution id
     * @return contribution
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return contribution by id.")
    public ContributionDto getContributionById(@PathVariable Long id) {
        return contributionService.getById(id).dto();
    }

    /**
     * List contributions for a participant.
     * @param participantId participant id
     * @return list of contributions
     */
    @GetMapping("/participants/{participantId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return all contributions for a participant.")
    public List<ContributionDto> getContributionsByParticipantId(@PathVariable Long participantId) {
        return contributionService.getByParticipantId(participantId)
                .stream().map(c -> c.dto()).toList();
    }
}

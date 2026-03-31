package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.plan.PlanConfigurationDto;
import com.babakjan.moneybag.dto.plan.UpdatePlanConfigRequest;
import com.babakjan.moneybag.entity.PlanConfiguration;
import com.babakjan.moneybag.entity.PlanConfigurationAuditLog;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.service.PlanConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/plans", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Plan Configuration", description = "Plan-level configuration for SECURE 2.0 compliance")
public class PlanConfigurationController {

    private final PlanConfigurationService planConfigurationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new plan configuration.")
    public PlanConfigurationDto createPlanConfiguration(@RequestBody PlanConfigurationDto dto) {
        return planConfigurationService.createPlanConfiguration(dto).dto();
    }

    @GetMapping("/{planId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get plan configuration by planId.")
    public PlanConfigurationDto getPlanConfiguration(@PathVariable String planId) throws PlanNotFoundException {
        return planConfigurationService.getByPlanId(planId).dto();
    }

    @PutMapping("/{planId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update plan configuration with audit logging.")
    public PlanConfigurationDto updatePlanConfiguration(@PathVariable String planId,
                                                        @RequestBody UpdatePlanConfigRequest request)
            throws PlanNotFoundException {
        return planConfigurationService.updatePlanConfiguration(planId, request).dto();
    }

    @GetMapping("/{planId}/audit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get audit log for a plan.")
    public List<PlanConfigurationAuditLog> getAuditLog(@PathVariable String planId) {
        return planConfigurationService.getAuditLog(planId);
    }
}

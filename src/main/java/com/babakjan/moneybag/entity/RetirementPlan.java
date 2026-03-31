package com.babakjan.moneybag.entity;

import com.babakjan.moneybag.dto.contribution.RetirementPlanDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "retirement_plans")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RetirementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PlanType planType;

    @NotNull
    private String planName;

    @NotNull
    private String employerName;

    /**
     * Create data transfer object.
     * @return retirement plan dto
     */
    public RetirementPlanDto dto() {
        return RetirementPlanDto.builder()
                .id(id)
                .planType(planType)
                .planName(planName)
                .employerName(employerName)
                .build();
    }
}

package com.babakjan.moneybag.entity;

import com.babakjan.moneybag.dto.contribution.ContributionDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "contributions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull
    private Participant participant;

    @ManyToOne
    @NotNull
    private RetirementPlan plan;

    @NotNull
    private Double amount;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ContributionType contributionType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TaxTreatment taxTreatment;

    @NotNull
    private LocalDate payrollDate;

    /**
     * Create data transfer object.
     * @return contribution dto
     */
    public ContributionDto dto() {
        return ContributionDto.builder()
                .id(id)
                .participantId(participant != null ? participant.getId() : null)
                .planId(plan != null ? plan.getId() : null)
                .amount(amount)
                .contributionType(contributionType)
                .taxTreatment(taxTreatment)
                .payrollDate(payrollDate)
                .build();
    }
}

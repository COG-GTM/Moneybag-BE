package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "participant_profiles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    private Double priorYearFicaWages;

    private Double ficaWagesFromControlledGroup;

    private Boolean isNewHire;

    @Enumerated(EnumType.STRING)
    private PlanType employerPlanType;

    private Boolean specialCatchUpEligible;

    private Double specialCatchUpLimit;
}

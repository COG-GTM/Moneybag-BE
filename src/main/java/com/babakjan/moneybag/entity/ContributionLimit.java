package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contribution_limits")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContributionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private Integer taxYear;

    @NotNull
    private Double regularLimit;

    @NotNull
    private Double catchUpLimit;

    @NotNull
    private Double superCatchUpLimit;

    @NotNull
    private Double ficaWagesThreshold;
}

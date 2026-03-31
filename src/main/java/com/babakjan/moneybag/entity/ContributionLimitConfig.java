package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "contribution_limit_configs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContributionLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int planYear;

    private Double ficaWageThreshold;

    private Double standardCatchUpLimit;

    private Double superCatchUpLimit;

    private Double regularContributionLimit;

    @Temporal(TemporalType.DATE)
    private Date effectiveDate;
}

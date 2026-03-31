package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "contribution_summaries")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContributionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long participantId;

    private int planYear;

    private Double totalRegularPreTax;

    private Double totalRegularRoth;

    private Double totalCatchUpPreTax;

    private Double totalCatchUpRoth;

    private Double totalSuperCatchUpRoth;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    @PrePersist
    protected void onCreate() {
        lastUpdated = new Date();
        if (totalRegularPreTax == null) totalRegularPreTax = 0.0;
        if (totalRegularRoth == null) totalRegularRoth = 0.0;
        if (totalCatchUpPreTax == null) totalCatchUpPreTax = 0.0;
        if (totalCatchUpRoth == null) totalCatchUpRoth = 0.0;
        if (totalSuperCatchUpRoth == null) totalSuperCatchUpRoth = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = new Date();
    }
}

package com.babakjan.moneybag.entity;

import com.babakjan.moneybag.dto.plan.PlanConfigurationDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "plan_configurations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlanConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String planId;

    private String planName;

    private String planType;

    private Boolean rothCatchUpAvailable;

    @Temporal(TemporalType.DATE)
    private Date rothEffectiveDate;

    private Boolean governmentalSpecialCatchUp;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public PlanConfigurationDto dto() {
        return PlanConfigurationDto.builder()
                .planId(planId)
                .planName(planName)
                .planType(planType)
                .rothCatchUpAvailable(rothCatchUpAvailable)
                .rothEffectiveDate(rothEffectiveDate)
                .governmentalSpecialCatchUp(governmentalSpecialCatchUp)
                .build();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}

package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "plan_configuration_audit_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlanConfigurationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planId;

    private String fieldChanged;

    private String oldValue;

    private String newValue;

    private String changedBy;

    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = new Date();
        }
    }
}

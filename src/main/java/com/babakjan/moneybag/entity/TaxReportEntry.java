package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "tax_report_entries")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaxReportEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long participantId;

    private int planYear;

    private String planType;

    private Double regularRothAmount;

    private Double regularPreTaxAmount;

    private Double catchUpRothAmount;

    private Double catchUpPreTaxAmount;

    private Double superCatchUpRothAmount;

    private String w2Box12Code;

    private Double w2Box12Amount;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}

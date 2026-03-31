package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "payroll_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchId;

    private Long participantId;

    private String participantName;

    private Double amount;

    private String contributionType;

    private String designation;

    private String status;

    private String errorCode;

    private String errorMessage;

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date processedAt;
}

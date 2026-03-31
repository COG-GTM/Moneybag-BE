package com.babakjan.moneybag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "participant_elections")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipantElection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long participantId;

    private int planYear;

    private Double regularContributionAmount;

    private String regularDesignation;

    private Double catchUpContributionAmount;

    private String catchUpDesignation;

    private String catchUpType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date electionDate;

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (electionDate == null) {
            electionDate = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}

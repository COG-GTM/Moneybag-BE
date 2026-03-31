package com.babakjan.moneybag.entity;

import com.babakjan.moneybag.dto.participant.ParticipantDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "participants")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Double priorYearFicaWages;

    @ManyToOne
    @NotNull
    private User user;

    /**
     * Create data transfer object.
     * @return participant dto
     */
    public ParticipantDto dto() {
        return ParticipantDto.builder()
                .id(id)
                .dateOfBirth(dateOfBirth)
                .priorYearFicaWages(priorYearFicaWages)
                .userId(user != null ? user.getId() : null)
                .build();
    }
}

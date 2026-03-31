package com.babakjan.moneybag.dto.participant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateParticipantRequest {

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Double priorYearFicaWages;

    @NotNull
    private Long userId;
}

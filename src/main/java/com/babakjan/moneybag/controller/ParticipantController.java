package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.dto.participant.CreateParticipantRequest;
import com.babakjan.moneybag.dto.participant.ParticipantDto;
import com.babakjan.moneybag.error.exception.UserNotFoundException;
import com.babakjan.moneybag.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/participants", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Participant", description = "Retirement plan participants")
@SecurityRequirement(name = "bearer-key")
public class ParticipantController {

    private final ParticipantService participantService;

    /**
     * Create a new participant.
     * @param request participant data
     * @return created participant
     * @throws UserNotFoundException if user not found
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new participant.")
    public ParticipantDto createParticipant(@RequestBody @Valid CreateParticipantRequest request)
            throws UserNotFoundException {
        return participantService.save(request).dto();
    }

    /**
     * Get participant by id.
     * @param id participant id
     * @return participant
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return participant by id.")
    public ParticipantDto getParticipantById(@PathVariable Long id) {
        return participantService.getById(id).dto();
    }

    /**
     * Get all participants.
     * @return list of participants
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return all participants.")
    public List<ParticipantDto> getAllParticipants() {
        return participantService.getAll().stream().map(p -> p.dto()).toList();
    }

    /**
     * Delete participant by id.
     * @param id participant id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete participant by id.")
    public void deleteParticipant(@PathVariable Long id) {
        participantService.deleteById(id);
    }
}

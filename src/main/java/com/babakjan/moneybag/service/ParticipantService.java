package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.participant.CreateParticipantRequest;
import com.babakjan.moneybag.entity.Participant;
import com.babakjan.moneybag.entity.User;
import com.babakjan.moneybag.error.exception.UserNotFoundException;
import com.babakjan.moneybag.repository.ParticipantRepository;
import com.babakjan.moneybag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    private final UserRepository userRepository;

    /**
     * Create a new participant.
     * @param request participant data
     * @return saved participant
     * @throws UserNotFoundException if user not found
     */
    public Participant save(CreateParticipantRequest request) throws UserNotFoundException {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(request.getUserId());
        }

        Participant participant = Participant.builder()
                .dateOfBirth(request.getDateOfBirth())
                .priorYearFicaWages(request.getPriorYearFicaWages())
                .user(userOpt.get())
                .build();

        return participantRepository.save(participant);
    }

    /**
     * Get participant by id.
     * @param id participant id
     * @return participant
     * @throws IllegalArgumentException if not found
     */
    public Participant getById(Long id) {
        Optional<Participant> participantOpt = participantRepository.findById(id);
        if (participantOpt.isEmpty()) {
            throw new IllegalArgumentException("Participant of id: " + id + " not found.");
        }
        return participantOpt.get();
    }

    /**
     * Get all participants.
     * @return list of participants
     */
    public List<Participant> getAll() {
        return participantRepository.findAll();
    }

    /**
     * Delete participant by id.
     * @param id participant id
     */
    public void deleteById(Long id) {
        if (!participantRepository.existsById(id)) {
            throw new IllegalArgumentException("Participant of id: " + id + " not found.");
        }
        participantRepository.deleteById(id);
    }
}

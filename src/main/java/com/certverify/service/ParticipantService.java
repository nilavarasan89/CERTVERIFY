package com.certverify.service;

import com.certverify.dto.ParticipantRequest;
import com.certverify.dto.ParticipantResponse;
import com.certverify.entity.Participant;
import com.certverify.exception.DuplicateResourceException;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.ParticipantRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    @Transactional
    public ParticipantResponse createParticipant(ParticipantRequest request) {
        if (participantRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Participant email already exists: " + request.getEmail());
        }

        Participant participant = Participant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        Participant saved = participantRepository.save(participant);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getAllParticipants() {
        return participantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParticipantResponse getParticipantById(Long id) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + id));
        return mapToResponse(participant);
    }

    public Participant mapToEntity(ParticipantResponse response) {
        return Participant.builder()
                .id(response.getId())
                .name(response.getName())
                .email(response.getEmail())
                .phone(response.getPhone())
                .build();
    }

    public ParticipantResponse mapToResponse(Participant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .name(participant.getName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .build();
    }
}

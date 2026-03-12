package com.konrad.konradquiz.service.interfaces;

import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.entity.Participant;

public interface IParticipantService {
    ExperimentSessionDto register(ParticipantRequestDto dto);
    ParticipantResponseDto findById(Long id);
    Participant findEntityById(Long id);// ← returns raw entity for internal service use
    void recordCompletionTime(Long participantId, Long completionTimeSeconds);
}
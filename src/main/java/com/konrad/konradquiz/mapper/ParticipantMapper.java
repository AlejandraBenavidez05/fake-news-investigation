package com.konrad.konradquiz.mapper;

import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.entity.Participant;
import org.springframework.stereotype.Component;

@Component
public class ParticipantMapper {

    public ParticipantResponseDto toResponseDto(Participant participant) {
        return ParticipantResponseDto.builder()
                .id(participant.getId())
                .alias(participant.getAlias())
                .email(participant.getEmail())
                .sex(participant.getSex())
                .age(participant.getAge())
                .region(participant.getRegion())
                .completionTimeSeconds(participant.getCompletionTimeSeconds())
                .registeredAt(participant.getRegisteredAt())
                .feedbackTiming(participant.getFeedbackTiming())
                .presentationFormat(participant.getPresentationFormat())
                .build();
    }
}
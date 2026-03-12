package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Participant.ExperimentGroup;
import com.konrad.konradquiz.entity.Participant.Sex;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ParticipantResponseDto {
    private Long id;
    private String alias;
    private String email;
    private Sex sex;
    private Integer age;
    private ExperimentGroup experimentGroup;
    private LocalDateTime registeredAt;
    private String region;
    private Long completionTimeSeconds;
}
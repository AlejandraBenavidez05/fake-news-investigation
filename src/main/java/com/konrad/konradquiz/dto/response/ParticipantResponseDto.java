package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Participant.Sex;
import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
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
    private LocalDateTime registeredAt;
    private String region;
    private Long completionTimeSeconds;
    private FeedbackTiming feedbackTiming;
    private PresentationFormat presentationFormat;
}
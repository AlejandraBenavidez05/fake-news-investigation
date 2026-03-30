package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import lombok.Builder;
import lombok.Data;
import java.util.List;


@Data
@Builder
public class ExperimentSessionDto {
    private Long participantId;
    private String alias;
    private List<QuestionSessionDto> questions;  // includes correctAnswer + references
    private FeedbackTiming feedbackTiming;
    private PresentationFormat presentationFormat;
}
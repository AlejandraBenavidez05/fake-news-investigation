package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExperimentSessionDto {
    private Long participantId;
    private String alias;
    private FeedbackTiming feedbackTiming;
    private PresentationFormat presentationFormat;
    private Question.NewsSet newsSet;

    // Three separate lists — frontend knows exactly what to render
    private List<QuestionSessionDto> profileQuestions;      // always TEXT
    private List<QuestionSessionDto> newsPart1Questions;    // format from assignment
    private List<QuestionSessionDto> newsPart2Questions;    // always TEXT, memory test
}
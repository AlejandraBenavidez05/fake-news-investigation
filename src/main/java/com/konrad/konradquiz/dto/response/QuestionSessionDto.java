package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Question.CorrectAnswer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionSessionDto {
    private String questionCode;
    private String itemText;
    private Integer assignedOrder;
    // Reveal data included from the start — frontend controls when to show it
    private CorrectAnswer correctAnswer;
    private String referenceApa;
    private String supportingQuote;
}
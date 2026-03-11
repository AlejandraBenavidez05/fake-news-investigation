package com.konrad.konradquiz.mapper;

import com.konrad.konradquiz.dto.response.QuestionSessionDto;
import com.konrad.konradquiz.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    // Only one method now — used at registration
    public QuestionSessionDto toSessionDto(Question question, int assignedOrder) {
        return QuestionSessionDto.builder()
                .questionCode(question.getQuestionCode())
                .itemText(question.getItemText())
                .assignedOrder(assignedOrder)
                .correctAnswer(question.getCorrectAnswer())
                .referenceApa(question.getReferenceApa())
                .supportingQuote(question.getSupportingQuote())
                .build();
    }
}
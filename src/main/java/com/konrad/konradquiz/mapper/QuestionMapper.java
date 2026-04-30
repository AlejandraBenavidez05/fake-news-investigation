package com.konrad.konradquiz.mapper;

import com.konrad.konradquiz.dto.response.QuestionSessionDto;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    // Only one method now — used at registration
    public QuestionSessionDto toSessionDto(Question question, int assignedOrder,
                                           Participant.PresentationFormat format) {
        return QuestionSessionDto.builder()
                .questionCode(question.getQuestionCode())
                .questionType(question.getQuestionType().name())
                .itemText(question.getItemText())
                .assignedOrder(assignedOrder)
                .presentationFormat(format.name())
                .correctAnswer(question.getCorrectAnswer() != null
                        ? question.getCorrectAnswer().name() : null)
                .referenceApa(question.getReferenceApa())
                .supportingQuote(question.getSupportingQuote())
                // News-specific
                .phase(question.getPhase())
                .category(question.getCategory())
                .novelty(question.getNovelty())
                .sourceVerificationUrl(question.getSourceVerificationUrl())
                .factCheckUrl(question.getFactCheckUrl())
                .originName(question.getOriginName())
                .fileName(question.getFileName())
                // Profile-specific
                .scaleOptions(question.getScaleOptions())
                .build();
    }
}
package com.konrad.konradquiz.mapper;

import com.konrad.konradquiz.dto.response.AnswerResponseDto;
import com.konrad.konradquiz.entity.Answer;
import org.springframework.stereotype.Component;

@Component
public class AnswerMapper {

    public AnswerResponseDto toResponseDto(Answer answer) {
        return AnswerResponseDto.builder()
                .id(answer.getId())
                .questionCode(answer.getQuestion().getQuestionCode())
                .score(answer.getScore())
                .questionOrder(answer.getQuestionOrder())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }
}
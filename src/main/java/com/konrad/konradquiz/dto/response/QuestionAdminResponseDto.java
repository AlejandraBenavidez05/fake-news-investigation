package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Question.CorrectAnswer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionAdminResponseDto {
    private String questionCode;
    private String itemText;
    private CorrectAnswer correctAnswer;  // confirms what was just saved
}
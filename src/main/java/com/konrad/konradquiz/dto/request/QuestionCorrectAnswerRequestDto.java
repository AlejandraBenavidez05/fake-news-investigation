package com.konrad.konradquiz.dto.request;

import com.konrad.konradquiz.entity.Question.CorrectAnswer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionCorrectAnswerRequestDto {

    @NotNull
    private CorrectAnswer correctAnswer;  // FAKE or REAL
}
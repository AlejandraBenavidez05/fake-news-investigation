package com.konrad.konradquiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BatchAnswerRequestDto {

    @NotEmpty
    @Valid                          // triggers validation on each item in the list
    private List<AnswerRequestDto> answers;

    @NotNull
    private Long startedAt;    // epoch seconds — frontend sends when participant saw first question
}
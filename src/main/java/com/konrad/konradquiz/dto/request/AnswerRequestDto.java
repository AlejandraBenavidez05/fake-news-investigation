package com.konrad.konradquiz.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
@Data
public class AnswerRequestDto {

    @NotBlank
    private String questionCode;

    @NotNull
    @Min(0) @Max(100)
    private Integer score;

    @NotNull
    @Min(1)
    private Integer questionOrder;
}
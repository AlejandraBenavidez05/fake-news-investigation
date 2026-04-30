package com.konrad.konradquiz.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AnswerResponseDto {
    private Long id;
    private String questionCode;
    private Integer score;
    private Integer questionOrder;
    private LocalDateTime answeredAt;
}
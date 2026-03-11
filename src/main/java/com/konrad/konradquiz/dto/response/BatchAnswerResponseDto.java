package com.konrad.konradquiz.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchAnswerResponseDto {
    private Long participantId;
    private int savedCount;         // confirms how many answers were persisted
}
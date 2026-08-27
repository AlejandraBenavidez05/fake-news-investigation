package com.konrad.konradquiz.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RewardResponseDto {
    private Long participantId;
    private Integer rewardId;
    private String message;
}
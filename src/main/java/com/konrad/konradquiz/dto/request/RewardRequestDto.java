package com.konrad.konradquiz.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RewardRequestDto {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer rewardId;
}
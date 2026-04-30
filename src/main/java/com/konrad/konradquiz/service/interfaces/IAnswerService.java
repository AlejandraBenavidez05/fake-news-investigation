package com.konrad.konradquiz.service.interfaces;

import com.konrad.konradquiz.dto.request.BatchAnswerRequestDto;
import com.konrad.konradquiz.dto.response.BatchAnswerResponseDto;

public interface IAnswerService {
    BatchAnswerResponseDto submitBatch(Long participantId, BatchAnswerRequestDto dto);
}
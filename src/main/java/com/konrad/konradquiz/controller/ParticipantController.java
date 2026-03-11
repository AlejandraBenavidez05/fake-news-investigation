package com.konrad.konradquiz.controller;

import com.konrad.konradquiz.dto.request.BatchAnswerRequestDto;
import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.response.BatchAnswerResponseDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.service.interfaces.IAnswerService;
import com.konrad.konradquiz.service.interfaces.IParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final IParticipantService participantService;
    private final IAnswerService answerService;

    @PostMapping("/register")
    public ResponseEntity<ExperimentSessionDto> register(
            @Valid @RequestBody ParticipantRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(participantService.register(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipantResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(participantService.findById(id));
    }

    @PostMapping("/{id}/answers/batch")
    public ResponseEntity<BatchAnswerResponseDto> submitBatch(
            @PathVariable Long id,
            @Valid @RequestBody BatchAnswerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(answerService.submitBatch(id, dto));
    }
}
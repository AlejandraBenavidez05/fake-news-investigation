package com.konrad.konradquiz.controller;

import com.konrad.konradquiz.dto.request.QuestionCorrectAnswerRequestDto;
import com.konrad.konradquiz.dto.response.QuestionAdminResponseDto;
import com.konrad.konradquiz.service.interfaces.IExportService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IExportService exportService;
    private final IQuestionService questionService;

    @PatchMapping("/questions/{questionCode}/answer")  // ← uncommented
    public ResponseEntity<QuestionAdminResponseDto> updateCorrectAnswer(
            @PathVariable String questionCode,
            @Valid @RequestBody QuestionCorrectAnswerRequestDto dto) {
        return ResponseEntity.ok(questionService.updateCorrectAnswer(questionCode, dto));
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) {
        exportService.exportToCsv(response);
    }
}
package com.konrad.konradquiz.dto.response;

import com.konrad.konradquiz.entity.Question.CorrectAnswer;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class QuestionSessionDto {
    // Common
    private String questionCode;
    private String questionType;        // PROFILE or NEWS
    private String itemText;
    private Integer assignedOrder;
    private String presentationFormat;  // what format to render this specific question

    // Reveal data — frontend controls when to show based on feedbackTiming
    private String correctAnswer;
    private String referenceApa;
    private String supportingQuote;

    // News-specific — null for PROFILE questions
    private String phase;
    private String category;
    private String novelty;
    private String sourceVerificationUrl;
    private String factCheckUrl;
    private String originName;
    private String fileName;

    // Profile-specific — null for NEWS questions
    private String scaleOptions;        // "nada,poco,bastante,mucho"
}
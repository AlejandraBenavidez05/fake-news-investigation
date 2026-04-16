package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.ExportException;
import com.konrad.konradquiz.repository.AnswerRepository;
import com.konrad.konradquiz.service.interfaces.IExportService;
import jakarta.servlet.http.HttpServletResponse;
import com.konrad.konradquiz.entity.Answer.SdtCategory;
import com.konrad.konradquiz.util.SdtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements IExportService {

    private final AnswerRepository answerRepository;

    @Override
    public void exportToCsv(HttpServletResponse response) {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"konradquiz_export.csv\"");

        try {
            List<Answer> answers = answerRepository.findAllForExport();
            PrintWriter writer = response.getWriter();

            writer.println(
                    // Participant
                    "participantId,alias,email,sex,age,region,feedbackTiming,presentationFormat," +
                            "completionTimeSeconds,registeredAt," +
                            // Consents
                            "consentAcademicPurpose,consentParticipationProcess,consentDataProcessing," +
                            "consentNoRisk,consentNoPayment,consentProjectInfo," +
                            // Question
                            "questionCode,questionType,constructo,subCategory,subCategory2,itemText," +
                            "correctAnswer,referenceApa,supportingQuote,phase,category,novelty," +
                            "sourceVerificationUrl,factCheckUrl,originName,fileName,scaleOptions," +
                            // Answer
                            "score,questionOrder,answeredAt,isCorrect,sdtCategory"
            );
            for (Answer answer : answers) {
                Participant p = answer.getParticipant();
                Question q   = answer.getQuestion();

                boolean isCorrect = q.getCorrectAnswer() != null && (
                        (q.getCorrectAnswer() == Question.CorrectAnswer.REAL && answer.getScore() >= 50) ||
                                (q.getCorrectAnswer() == Question.CorrectAnswer.FAKE && answer.getScore() < 50)
                );

                SdtCategory sdt = SdtUtil.classify(q.getCorrectAnswer(), answer.getScore());

                writer.println(String.join(",",
                        sanitize(p.getId()),
                        sanitize(p.getAlias()),
                        sanitize(p.getEmail()),
                        sanitize(p.getSex()),
                        sanitize(p.getAge()),
                        sanitize(p.getRegion()),
                        sanitize(p.getCompletionTimeSeconds()),
                        sanitize(p.getRegisteredAt()),
                        sanitize(q.getQuestionCode()),
                        sanitize(q.getConstructo()),
                        sanitize(q.getSubCategory()),
                        sanitize(q.getSubCategory2()),
                        sanitize(q.getItemText()),
                        sanitize(q.getCorrectAnswer()),
                        sanitize(q.getReferenceApa()),
                        sanitize(q.getSupportingQuote()),
                        sanitize(q.getPhase()),
                        sanitize(q.getCategory()),
                        sanitize(q.getNovelty()),
                        sanitize(q.getSourceVerificationUrl()),
                        sanitize(q.getFactCheckUrl()),
                        sanitize(q.getOriginName()),
                        sanitize(q.getFileName()),
                        sanitize(q.getScaleOptions()),
                        sanitize(answer.getScore()),
                        sanitize(answer.getQuestionOrder()),
                        sanitize(answer.getAnsweredAt()),
                        sanitize(isCorrect),
                        sanitize(sdt)  ,
                        sanitize(p.getFeedbackTiming()),
                        sanitize(p.getPresentationFormat()),
                        sanitize(p.getConsentAcademicPurpose()),
                        sanitize(p.getConsentParticipationProcess()),
                        sanitize(p.getConsentDataProcessing()),
                        sanitize(p.getConsentNoRisk()),
                        sanitize(p.getConsentNoPayment()),
                        sanitize(p.getConsentProjectInfo())
                ));
            }

            writer.flush();

        } catch (IOException ex) {
            throw new ExportException("Failed to write CSV export to response stream.", ex);
        }
    }

    private String sanitize(Object value) {
        if (value == null) return "\"\"";
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }
}
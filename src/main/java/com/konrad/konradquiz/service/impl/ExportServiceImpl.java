package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Answer.SdtCategory;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.ExportException;
import com.konrad.konradquiz.repository.AnswerRepository;
import com.konrad.konradquiz.service.interfaces.IExportService;
import com.konrad.konradquiz.util.SdtUtil;
import jakarta.servlet.http.HttpServletResponse;
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

            // ── Header — order must match row exactly ────────────────────────
            writer.println(
                    // Participant
                    "participantId," +
                            "alias," +
                            "email," +
                            "sex," +
                            "age," +
                            "region," +
                            "feedbackTiming," +
                            "presentationFormat," +
                            "newsSet," +
                            "completionTimeSeconds," +
                            "registeredAt," +
                            // Consents
                            "consentAcademicPurpose," +
                            "consentParticipationProcess," +
                            "consentDataProcessing," +
                            "consentNoRisk," +
                            "consentNoPayment," +
                            "consentProjectInfo," +
                            "rewardId," +
                            // Question
                            "questionCode," +
                            "questionType," +
                            "constructo," +
                            "subCategory," +
                            "subCategory2," +
                            "itemText," +
                            "correctAnswer," +
                            "referenceApa," +
                            "supportingQuote," +
                            "phase," +
                            "category," +
                            "novelty," +
                            "sourceVerificationUrl," +
                            "factCheckUrl," +
                            "originName," +
                            "fileName," +
                            "scaleOptions," +
                            // Answer
                            "score," +
                            "questionOrder," +
                            "answeredAt," +
                            "answerType," +
                            // Computed
                            "isCorrect," +
                            "sdtCategory"
            );

            for (Answer answer : answers) {
                Participant p = answer.getParticipant();
                Question    q = answer.getQuestion();

                boolean isCorrect = computeIsCorrect(answer, q);

                SdtCategory sdt = answer.getSdtCategory();

                writer.println(String.join(",",
                        // ── Participant ──────────────────────────────────────
                        sanitize(p.getId()),
                        sanitize(p.getAlias()),
                        sanitize(p.getEmail()),
                        sanitize(p.getSex()),
                        sanitize(p.getAge()),
                        sanitize(p.getRegion()),
                        sanitize(p.getFeedbackTiming()),
                        sanitize(p.getPresentationFormat()),
                        sanitize(p.getNewsSet()),
                        sanitize(p.getCompletionTimeSeconds()),
                        sanitize(p.getRegisteredAt()),
                        // ── Consents ─────────────────────────────────────────
                        sanitize(p.getConsentAcademicPurpose()),
                        sanitize(p.getConsentParticipationProcess()),
                        sanitize(p.getConsentDataProcessing()),
                        sanitize(p.getConsentNoRisk()),
                        sanitize(p.getConsentNoPayment()),
                        sanitize(p.getConsentProjectInfo()),
                        sanitize(p.getRewardId()),
                        // ── Question ─────────────────────────────────────────
                        sanitize(q.getQuestionCode()),
                        sanitize(q.getQuestionType()),
                        sanitize(q.getConstructo()),
                        sanitize(q.getSubCategory()),
                        sanitize(q.getSubCategory2()),
                        sanitize(q.getItemText()),
                        sanitize(q.getCorrectAnswer()),
                        sanitize(q.getReferenceApa()),
                        sanitize(q.getSupportingQuote()),
                        sanitize(q.getPhase()),
                        sanitize(q.getCategory()),
                        sanitize(answer.getNovelty()),
                        sanitize(q.getSourceVerificationUrl()),
                        sanitize(q.getFactCheckUrl()),
                        sanitize(q.getOriginName()),
                        sanitize(q.getFileName()),
                        sanitize(q.getScaleOptions()),
                        // ── Answer ───────────────────────────────────────────
                        sanitize(answer.getScore()),
                        sanitize(answer.getQuestionOrder()),
                        sanitize(answer.getAnsweredAt()),
                        sanitize(answer.getAnswerType()),
                        // ── Computed ─────────────────────────────────────────
                        sanitize(isCorrect),
                        sanitize(sdt)
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

    private boolean computeIsCorrect(Answer answer, Question question) {
        if (question.getCorrectAnswer() == null) return false;

        return switch (answer.getAnswerType()) {
            case PROFILE -> false;

            case FAKE_DETECTION ->
                // -10 to 10 scale, negative = FAKE, positive = REAL
                    (question.getCorrectAnswer() == Question.CorrectAnswer.REAL && answer.getScore() > 0) ||
                            (question.getCorrectAnswer() == Question.CorrectAnswer.FAKE && answer.getScore() < 0);

            case MEMORY_TEST ->
                    answer.getNovelty() != null && (
                            (answer.getNovelty().equals("N") && answer.getScore() > 0) ||
                                    (answer.getNovelty().equals("V") && answer.getScore() < 0)
                    );
        };
    }
}
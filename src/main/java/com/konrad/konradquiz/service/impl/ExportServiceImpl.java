package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.ExportException;
import com.konrad.konradquiz.repository.AnswerRepository;
import com.konrad.konradquiz.service.interfaces.IExportService;
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

            // ── All columns from all three tables ────────────────────────────
            writer.println(
                    // participants table
                    "participantId," +
                            "alias," +
                            "email," +
                            "sex," +
                            "age," +
                            "region," +
                            "experimentGroup," +
                            "completionTimeSeconds," +
                            "registeredAt," +
                            // questions table
                            "questionCode," +
                            "constructo," +
                            "subCategory," +
                            "subCategory2," +
                            "itemText," +
                            "correctAnswer," +
                            "referenceApa," +
                            "supportingQuote," +
                            // answers table
                            "score," +
                            "questionOrder," +
                            "answeredAt," +
                            // computed
                            "isCorrect"
            );

            for (Answer answer : answers) {
                Participant p = answer.getParticipant();
                Question q   = answer.getQuestion();

                boolean isCorrect = q.getCorrectAnswer() != null && (
                        (q.getCorrectAnswer() == Question.CorrectAnswer.REAL && answer.getScore() >= 50) ||
                                (q.getCorrectAnswer() == Question.CorrectAnswer.FAKE && answer.getScore() < 50)
                );

                writer.println(String.join(",",
                        // ── Participant ──────────────────────────────────────
                        sanitize(p.getId()),
                        sanitize(p.getAlias()),
                        sanitize(p.getEmail()),              // decrypted via @Convert ✅
                        sanitize(p.getSex()),
                        sanitize(p.getAge()),
                        sanitize(p.getRegion()),
                        sanitize(p.getExperimentGroup()),
                        sanitize(p.getCompletionTimeSeconds()),
                        sanitize(p.getRegisteredAt()),
                        // ── Question ─────────────────────────────────────────
                        sanitize(q.getQuestionCode()),
                        sanitize(q.getConstructo()),
                        sanitize(q.getSubCategory()),
                        sanitize(q.getSubCategory2()),
                        sanitize(q.getItemText()),
                        sanitize(q.getCorrectAnswer()),
                        sanitize(q.getReferenceApa()),
                        sanitize(q.getSupportingQuote()),
                        // ── Answer ───────────────────────────────────────────
                        sanitize(answer.getScore()),
                        sanitize(answer.getQuestionOrder()),
                        sanitize(answer.getAnsweredAt()),
                        // ── Computed ─────────────────────────────────────────
                        sanitize(isCorrect)
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
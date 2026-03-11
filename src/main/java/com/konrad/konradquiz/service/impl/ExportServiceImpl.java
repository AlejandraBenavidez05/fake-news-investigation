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
            // Loads full entities — @Convert decrypts email automatically ✅
            List<Answer> answers = answerRepository.findAllForExport();
            PrintWriter writer = response.getWriter();

            writer.println(
                    "participantId,alias,email,sex,age,experimentGroup," +
                            "questionCode,constructo,subCategory,subCategory2," +
                            "itemText,correctAnswer,score,questionOrder,answeredAt,isCorrect"
            );

            for (Answer answer : answers) {
                Participant p = answer.getParticipant();  // email already decrypted ✅
                Question q   = answer.getQuestion();

                // Fix — null check first
                boolean isCorrect = q.getCorrectAnswer() != null && (
                        (q.getCorrectAnswer() == Question.CorrectAnswer.REAL && answer.getScore() >= 50) ||
                                (q.getCorrectAnswer() == Question.CorrectAnswer.FAKE && answer.getScore() < 50)
                );

                writer.println(String.join(",",
                        sanitize(p.getId()),
                        sanitize(p.getAlias()),
                        sanitize(p.getEmail()),                  // plain text ✅ @Convert ran
                        sanitize(p.getSex().name()),
                        sanitize(p.getAge()),
                        sanitize(p.getExperimentGroup().name()),
                        sanitize(q.getQuestionCode()),
                        sanitize(q.getConstructo()),
                        sanitize(q.getSubCategory()),
                        sanitize(q.getSubCategory2()),
                        sanitize(q.getItemText()),
                        sanitize(q.getCorrectAnswer()),
                        sanitize(answer.getScore()),
                        sanitize(answer.getQuestionOrder()),
                        sanitize(answer.getAnsweredAt()),
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
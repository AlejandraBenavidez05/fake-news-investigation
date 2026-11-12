package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Answer;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CSV export of every answer joined with its participant and question.
 *
 * <p>The admin dashboard of the Angular frontend parses this file by header name, so column names,
 * value formatting and quoting are part of the API contract.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements IExportService {

    /** Column order of the export: header row and data rows are both generated from this list. */
    private static final List<CsvColumn> COLUMNS = List.of(
            // Participant
            column("participantId", a -> a.getParticipant().getId()),
            column("alias", a -> a.getParticipant().getAlias()),
            column("email", a -> a.getParticipant().getEmail()),
            column("sex", a -> a.getParticipant().getSex()),
            column("age", a -> a.getParticipant().getAge()),
            column("region", a -> a.getParticipant().getRegion()),
            column("feedbackTiming", a -> a.getParticipant().getFeedbackTiming()),
            column("presentationFormat", a -> a.getParticipant().getPresentationFormat()),
            column("newsSet", a -> a.getParticipant().getNewsSet()),
            column("completionTimeSeconds", a -> a.getParticipant().getCompletionTimeSeconds()),
            column("registeredAt", a -> a.getParticipant().getRegisteredAt()),
            // Consents
            column("consentAcademicPurpose", a -> a.getParticipant().getConsentAcademicPurpose()),
            column("consentParticipationProcess", a -> a.getParticipant().getConsentParticipationProcess()),
            column("consentDataProcessing", a -> a.getParticipant().getConsentDataProcessing()),
            column("consentNoRisk", a -> a.getParticipant().getConsentNoRisk()),
            column("consentNoPayment", a -> a.getParticipant().getConsentNoPayment()),
            column("consentProjectInfo", a -> a.getParticipant().getConsentProjectInfo()),
            // Question
            column("questionCode", a -> a.getQuestion().getQuestionCode()),
            column("questionType", a -> a.getQuestion().getQuestionType()),
            column("constructo", a -> a.getQuestion().getConstructo()),
            column("subCategory", a -> a.getQuestion().getSubCategory()),
            column("subCategory2", a -> a.getQuestion().getSubCategory2()),
            column("itemText", a -> a.getQuestion().getItemText()),
            column("correctAnswer", a -> a.getQuestion().getCorrectAnswer()),
            column("referenceApa", a -> a.getQuestion().getReferenceApa()),
            column("supportingQuote", a -> a.getQuestion().getSupportingQuote()),
            column("phase", a -> a.getQuestion().getPhase()),
            column("category", a -> a.getQuestion().getCategory()),
            column("novelty", a -> a.getQuestion().getNovelty()),
            column("sourceVerificationUrl", a -> a.getQuestion().getSourceVerificationUrl()),
            column("factCheckUrl", a -> a.getQuestion().getFactCheckUrl()),
            column("originName", a -> a.getQuestion().getOriginName()),
            column("fileName", a -> a.getQuestion().getFileName()),
            column("scaleOptions", a -> a.getQuestion().getScaleOptions()),
            // Answer
            column("score", Answer::getScore),
            column("questionOrder", Answer::getQuestionOrder),
            column("answeredAt", Answer::getAnsweredAt),
            column("answerType", Answer::getAnswerType),
            // Computed
            column("isCorrect", ExportServiceImpl::isCorrect),
            column("sdtCategory", a -> SdtUtil.classify(a.getQuestion().getCorrectAnswer(), a.getScore()))
    );

    private static final String HEADER_ROW = COLUMNS.stream()
            .map(CsvColumn::header)
            .collect(Collectors.joining(","));

    private final AnswerRepository answerRepository;

    @Override
    public void exportToCsv(HttpServletResponse response) {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"konradquiz_export.csv\"");

        try {
            List<Answer> answers = answerRepository.findAllForExport();
            PrintWriter writer = response.getWriter();

            writer.println(HEADER_ROW);
            for (Answer answer : answers) {
                writer.println(toCsvRow(answer));
            }
            writer.flush();
        } catch (IOException ex) {
            throw new ExportException("Failed to write CSV export to response stream.", ex);
        }
    }

    private static String toCsvRow(Answer answer) {
        return COLUMNS.stream()
                .map(column -> quote(column.value().apply(answer)))
                .collect(Collectors.joining(","));
    }

    /**
     * A score of exactly 50 counts as "REAL", so it is correct for REAL news and incorrect for FAKE news.
     * Questions without a defined correct answer are never correct.
     */
    private static boolean isCorrect(Answer answer) {
        Question.CorrectAnswer correctAnswer = answer.getQuestion().getCorrectAnswer();
        return correctAnswer != null && (
                (correctAnswer == Question.CorrectAnswer.REAL && answer.getScore() >= 50)
                        || (correctAnswer == Question.CorrectAnswer.FAKE && answer.getScore() < 50));
    }

    /** Every value is double-quoted (embedded quotes doubled); {@code null} becomes an empty quoted value. */
    private static String quote(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }

    private static CsvColumn column(String header, Function<Answer, Object> value) {
        return new CsvColumn(header, value);
    }

    private record CsvColumn(String header, Function<Answer, Object> value) {
    }
}

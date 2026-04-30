package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.dto.response.QuestionSessionDto;
import com.konrad.konradquiz.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionBuilderService {

    private final QuestionMapper questionMapper;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int SET_SIZE      = 12;
    public static final int PART1_SIZE    = 8;
    private static final int PART2_REPEAT  = 4;   // repeated from Part 1
    private static final int PART2_NEW     = 4;   // NOT shown in Part 1
    public static final int PART2_SIZE = 8;  // 4 new + 4 repeated

    /**
     * Builds the full session question lists.
     *
     * @param profileQuestions  all PROFILE questions
     * @param newsQuestions     all 12 NEWS questions for the assigned set
     * @param format            participant's assigned presentation format
     * @return built session
     */
    public BuiltSession build(
            List<Question> profileQuestions,
            List<Question> newsQuestions,
            PresentationFormat format
    ) {
        if (newsQuestions.size() != SET_SIZE) {
            throw new IllegalStateException(
                    "Expected " + SET_SIZE + " news questions for set, " +
                            "found: " + newsQuestions.size()
            );
        }

        // ── Shuffle and split news into Part 1 (8) and remaining (4) ────────
        List<Question> shuffled = new ArrayList<>(newsQuestions);
        Collections.shuffle(shuffled, SECURE_RANDOM);

        List<Question> part1Questions    = shuffled.subList(0, PART1_SIZE);       // 8 shown
        List<Question> notShownQuestions = shuffled.subList(PART1_SIZE, SET_SIZE); // 4 not shown

        // ── Part 2: 4 not shown + 4 randomly picked from Part 1 ─────────────
        List<Question> part1Copy = new ArrayList<>(part1Questions);
        Collections.shuffle(part1Copy, SECURE_RANDOM);
        List<Question> repeatedFromPart1 = part1Copy.subList(0, PART2_REPEAT);    // 4 repeated

        List<Question> part2Questions = new ArrayList<>();
        part2Questions.addAll(notShownQuestions);   // 4 never seen
        part2Questions.addAll(repeatedFromPart1);   // 4 seen in Part 1
        Collections.shuffle(part2Questions, SECURE_RANDOM); // mix them so order isn't obvious

        // ── Map to DTOs ──────────────────────────────────────────────────────
        List<QuestionSessionDto> profileDtos  = mapWithOrder(profileQuestions, PresentationFormat.TEXT,  1);
        List<QuestionSessionDto> part1Dtos    = mapWithOrder(part1Questions, format,                      1);
        List<QuestionSessionDto> part2Dtos    = mapWithOrder(part2Questions, format,     1);

        return new BuiltSession(profileDtos, part1Dtos, part2Dtos);
    }

    private List<QuestionSessionDto> mapWithOrder(
            List<Question> questions,
            PresentationFormat format,
            int startOrder
    ) {
        List<QuestionSessionDto> dtos = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            dtos.add(questionMapper.toSessionDto(questions.get(i), startOrder + i, format));
        }
        return dtos;
    }

    public record BuiltSession(
            List<QuestionSessionDto> profileQuestions,
            List<QuestionSessionDto> newsPart1Questions,
            List<QuestionSessionDto> newsPart2Questions
    ) {}
}
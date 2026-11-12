package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.response.QuestionSessionDto;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question;
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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** News questions shown in Part 1 (fake detection). */
    public static final int PART1_SIZE = 8;
    /** Part 2 (memory test) questions not shown in Part 1. */
    private static final int PART2_NEW = 4;
    /** Part 2 (memory test) questions repeated from Part 1. */
    private static final int PART2_REPEATED = 4;
    public static final int PART2_SIZE = PART2_NEW + PART2_REPEATED;
    /** News questions available per news set. */
    private static final int SET_SIZE = PART1_SIZE + PART2_NEW;

    private final QuestionMapper questionMapper;

    /**
     * Builds the participant's question lists.
     *
     * @param profileQuestions all PROFILE questions (always rendered as TEXT)
     * @param newsQuestions    the {@value #SET_SIZE} NEWS questions of the assigned set
     * @param format           participant's assigned presentation format for news questions
     */
    public BuiltSession build(List<Question> profileQuestions, List<Question> newsQuestions,
                              PresentationFormat format) {
        if (newsQuestions.size() != SET_SIZE) {
            throw new IllegalStateException(
                    "Expected " + SET_SIZE + " news questions for set, found: " + newsQuestions.size());
        }

        List<Question> shuffled = new ArrayList<>(newsQuestions);
        Collections.shuffle(shuffled, SECURE_RANDOM);
        List<Question> part1Questions = shuffled.subList(0, PART1_SIZE);
        List<Question> notShownInPart1 = shuffled.subList(PART1_SIZE, SET_SIZE);

        List<Question> part1Copy = new ArrayList<>(part1Questions);
        Collections.shuffle(part1Copy, SECURE_RANDOM);
        List<Question> repeatedFromPart1 = part1Copy.subList(0, PART2_REPEATED);

        List<Question> part2Questions = new ArrayList<>(notShownInPart1);
        part2Questions.addAll(repeatedFromPart1);
        Collections.shuffle(part2Questions, SECURE_RANDOM);   // so new and repeated items are interleaved

        return new BuiltSession(
                toSessionDtos(profileQuestions, PresentationFormat.TEXT),
                toSessionDtos(part1Questions, format),
                toSessionDtos(part2Questions, format));
    }

    /** Maps questions to DTOs numbering them 1..n in list order ({@code assignedOrder}). */
    private List<QuestionSessionDto> toSessionDtos(List<Question> questions, PresentationFormat format) {
        List<QuestionSessionDto> dtos = new ArrayList<>(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            dtos.add(questionMapper.toSessionDto(questions.get(i), i + 1, format));
        }
        return dtos;
    }

    public record BuiltSession(
            List<QuestionSessionDto> profileQuestions,
            List<QuestionSessionDto> newsPart1Questions,
            List<QuestionSessionDto> newsPart2Questions
    ) {
    }
}

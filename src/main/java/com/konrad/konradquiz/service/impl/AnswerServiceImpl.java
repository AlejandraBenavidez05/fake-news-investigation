package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.AnswerRequestDto;
import com.konrad.konradquiz.dto.request.BatchAnswerRequestDto;
import com.konrad.konradquiz.dto.response.BatchAnswerResponseDto;
import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Answer.AnswerType;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.BusinessException;
import com.konrad.konradquiz.repository.AnswerRepository;
import com.konrad.konradquiz.service.interfaces.IAnswerService;
import com.konrad.konradquiz.service.interfaces.IParticipantService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import com.konrad.konradquiz.util.SdtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerServiceImpl implements IAnswerService {

    private static final int PROFILE_MIN_SCORE = 0;
    private static final int PROFILE_MAX_SCORE = 100;
    /** FAKE_DETECTION and MEMORY_TEST share a -10..10 scale with 0 as "unsure". */
    private static final int NEWS_MIN_SCORE = -10;
    private static final int NEWS_MAX_SCORE = 10;

    private final AnswerRepository answerRepository;
    private final IParticipantService participantService;
    private final IQuestionService questionService;

    @Override
    public BatchAnswerResponseDto submitBatch(Long participantId, BatchAnswerRequestDto dto) {
        Participant participant = participantService.findEntityById(participantId);

        if (answerRepository.countByParticipantId(participantId) > 0) {
            throw new BusinessException("Participant has already submitted answers.");
        }

        validateSectionCounts(dto.getAnswers());

        List<Answer> answers = buildAnswers(participant, dto.getAnswers());
        answerRepository.saveAll(answers);

        long completionTimeSeconds = Instant.now().getEpochSecond() - dto.getStartedAt();
        participantService.recordCompletionTime(participantId, completionTimeSeconds);

        return BatchAnswerResponseDto.builder()
                .participantId(participantId)
                .savedCount(answers.size())
                .build();
    }

    /** Every profile question and exactly the Part 1 / Part 2 news questions of the session must be answered. */
    private void validateSectionCounts(List<AnswerRequestDto> answers) {
        long profileCount = countByType(answers, AnswerType.PROFILE);
        long part1Count = countByType(answers, AnswerType.FAKE_DETECTION);
        long part2Count = countByType(answers, AnswerType.MEMORY_TEST);

        long totalProfileQuestions = questionService.countByType(Question.QuestionType.PROFILE);

        if (profileCount != totalProfileQuestions) {
            throw new BusinessException(
                    "Must answer all " + totalProfileQuestions + " profile questions. Received: " + profileCount);
        }
        if (part1Count != SessionBuilderService.PART1_SIZE) {
            throw new BusinessException(
                    "Must answer exactly " + SessionBuilderService.PART1_SIZE
                            + " Part 1 news questions. Received: " + part1Count);
        }
        if (part2Count != SessionBuilderService.PART2_SIZE) {
            throw new BusinessException(
                    "Must answer exactly " + SessionBuilderService.PART2_SIZE
                            + " Part 2 news questions. Received: " + part2Count);
        }
    }

    private static long countByType(List<AnswerRequestDto> answers, AnswerType answerType) {
        return answers.stream().filter(answer -> answer.getAnswerType() == answerType).count();
    }

    /**
     * Answers are validated and resolved one by one in request order, so the first offending answer decides
     * the error (an invalid score -> 409, an unknown question code -> 404).
     */
    private List<Answer> buildAnswers(Participant participant, List<AnswerRequestDto> answerDtos) {
        Map<String, Question> questionsByCode = loadQuestions(answerDtos);
        List<Answer> answers = new ArrayList<>(answerDtos.size());
        for (AnswerRequestDto answerDto : answerDtos) {
            validateScoreRange(answerDto);
            Question question = resolveQuestion(questionsByCode, answerDto.getQuestionCode());
            answers.add(Answer.builder()
                    .participant(participant)
                    .question(question)
                    .score(answerDto.getScore())
                    .questionOrder(answerDto.getQuestionOrder())
                    .answerType(answerDto.getAnswerType())
                    .novelty(answerDto.getNovelty())
                    .sdtCategory(SdtUtil.classify(
                            question.getCorrectAnswer(), answerDto.getScore(), answerDto.getAnswerType()))
                    .build());
        }
        return answers;
    }

    private static void validateScoreRange(AnswerRequestDto dto) {
        int score = dto.getScore();
        if (dto.getAnswerType() == AnswerType.PROFILE) {
            if (score < PROFILE_MIN_SCORE || score > PROFILE_MAX_SCORE) {
                throw new BusinessException("Score for PROFILE must be between 0 and 100. "
                        + "Received: " + score + " for question " + dto.getQuestionCode());
            }
        } else if (score < NEWS_MIN_SCORE || score > NEWS_MAX_SCORE) {
            throw new BusinessException("Score for " + dto.getAnswerType() + " must be between -10 and 10. "
                    + "Received: " + score + " for question " + dto.getQuestionCode());
        }
    }

    /** Loads all referenced questions in a single query instead of one query per answer. */
    private Map<String, Question> loadQuestions(List<AnswerRequestDto> answerDtos) {
        Set<String> codes = answerDtos.stream()
                .map(AnswerRequestDto::getQuestionCode)
                .collect(Collectors.toSet());
        Map<String, Question> questionsByCode = new HashMap<>();
        for (Question question : questionService.findAllByCodes(codes)) {
            questionsByCode.put(question.getQuestionCode(), question);
        }
        return questionsByCode;
    }

    /**
     * Any code not matched exactly by the bulk lookup goes through the single-entity lookup, which keeps the
     * original semantics: database collation rules and the "Question not found: ..." 404 for unknown codes.
     */
    private Question resolveQuestion(Map<String, Question> questionsByCode, String questionCode) {
        Question question = questionsByCode.get(questionCode);
        return question != null ? question : questionService.findEntityByCode(questionCode);
    }
}

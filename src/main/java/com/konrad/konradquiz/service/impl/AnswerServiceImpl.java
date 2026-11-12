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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerServiceImpl implements IAnswerService {

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

    private List<Answer> buildAnswers(Participant participant, List<AnswerRequestDto> answerDtos) {
        Map<String, Question> questionsByCode = loadQuestions(answerDtos);
        return answerDtos.stream()
                .map(answerDto -> Answer.builder()
                        .participant(participant)
                        .question(resolveQuestion(questionsByCode, answerDto.getQuestionCode()))
                        .score(answerDto.getScore())
                        .questionOrder(answerDto.getQuestionOrder())
                        .answerType(answerDto.getAnswerType())
                        .build())
                .collect(Collectors.toList());
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
     * original semantics: database collation rules and the "Question not found: ..." 404 for unknown codes,
     * raised for the first unknown code in request order.
     */
    private Question resolveQuestion(Map<String, Question> questionsByCode, String questionCode) {
        Question question = questionsByCode.get(questionCode);
        return question != null ? question : questionService.findEntityByCode(questionCode);
    }
}

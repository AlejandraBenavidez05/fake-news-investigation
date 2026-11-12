package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.AnswerRequestDto;
import com.konrad.konradquiz.dto.request.BatchAnswerRequestDto;
import com.konrad.konradquiz.dto.response.BatchAnswerResponseDto;
import com.konrad.konradquiz.entity.Answer;
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
import java.util.List;

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

        // ── Validate counts per section ──────────────────────────────────────
        long profileCount = dto.getAnswers().stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.PROFILE)
                .count();
        long part1Count = dto.getAnswers().stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.FAKE_DETECTION)
                .count();
        long part2Count = dto.getAnswers().stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.MEMORY_TEST)
                .count();

        long totalProfileQuestions = questionService.countByType(Question.QuestionType.PROFILE);

        if (profileCount != totalProfileQuestions) {
            throw new BusinessException(
                    "Must answer all " + totalProfileQuestions + " profile questions. Received: " + profileCount
            );
        }
        if (part1Count != SessionBuilderService.PART1_SIZE) {
            throw new BusinessException(
                    "Must answer exactly " + SessionBuilderService.PART1_SIZE +
                            " Part 1 news questions. Received: " + part1Count
            );
        }
        if (part2Count != SessionBuilderService.PART2_SIZE) {
            throw new BusinessException(
                    "Must answer exactly " + SessionBuilderService.PART2_SIZE +
                            " Part 2 news questions. Received: " + part2Count
            );
        }

        // ── Build and save answers ───────────────────────────────────────────
        List<Answer> answers = new ArrayList<>();
        for (AnswerRequestDto answerDto : dto.getAnswers()) {
            validateScoreRange(answerDto);
            Question question = questionService.findEntityByCode(answerDto.getQuestionCode());
            answers.add(Answer.builder()
                    .participant(participant)
                    .question(question)
                    .score(answerDto.getScore())
                    .questionOrder(answerDto.getQuestionOrder())
                    .answerType(answerDto.getAnswerType())
                    .novelty(answerDto.getNovelty())
                    .sdtCategory(SdtUtil.classify(
                            question.getCorrectAnswer(),
                            answerDto.getScore(),
                            answerDto.getAnswerType()
                    ))
                    .build());
        }

        answerRepository.saveAll(answers);

        long completionTimeSeconds = Instant.now().getEpochSecond() - dto.getStartedAt();
        participantService.recordCompletionTime(participantId, completionTimeSeconds);

        return BatchAnswerResponseDto.builder()
                .participantId(participantId)
                .savedCount(answers.size())
                .build();
    }

    private void validateScoreRange(AnswerRequestDto dto) {
        switch (dto.getAnswerType()) {
            case PROFILE -> {
                if (dto.getScore() < 0 || dto.getScore() > 100) {
                    throw new BusinessException(
                            "Score for PROFILE must be between 0 and 100. " +
                                    "Received: " + dto.getScore() + " for question " + dto.getQuestionCode()
                    );
                }
            }
            case FAKE_DETECTION, MEMORY_TEST -> {
                if (dto.getScore() < -10 || dto.getScore() > 10) {
                    throw new BusinessException(
                            "Score for " + dto.getAnswerType() + " must be between -10 and 10. " +
                                    "Received: " + dto.getScore() + " for question " + dto.getQuestionCode()
                    );
                }
            }
        }
    }
}
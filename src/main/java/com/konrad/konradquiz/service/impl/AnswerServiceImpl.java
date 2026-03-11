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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Delegates to ParticipantService — no direct repo access
        Participant participant = participantService.findEntityById(participantId);

        // Business rule: participant cannot submit answers twice
        if (answerRepository.countByParticipantId(participantId) > 0) {
            throw new BusinessException("Participant has already submitted answers.");
        }

        // Business rule: must answer all questions
        long totalQuestions = questionService.countAll();
        if (dto.getAnswers().size() != totalQuestions) {
            throw new BusinessException(
                    "Must answer all " + totalQuestions + " questions. Received: " + dto.getAnswers().size()
            );
        }

        List<Answer> answers = new ArrayList<>();
        for (AnswerRequestDto answerDto : dto.getAnswers()) {

            // Delegates to QuestionService — no direct repo access
            Question question = questionService.findEntityByCode(answerDto.getQuestionCode());

            answers.add(Answer.builder()
                    .participant(participant)
                    .question(question)
                    .score(answerDto.getScore())
                    .questionOrder(answerDto.getQuestionOrder())
                    .build());
        }

        answerRepository.saveAll(answers);

        return BatchAnswerResponseDto.builder()
                .participantId(participantId)
                .savedCount(answers.size())
                .build();
    }
}
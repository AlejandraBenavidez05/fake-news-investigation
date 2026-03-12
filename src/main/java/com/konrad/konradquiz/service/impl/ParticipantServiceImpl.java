package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.dto.response.QuestionSessionDto;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.BusinessException;
import com.konrad.konradquiz.exception.ResourceNotFoundException;
import com.konrad.konradquiz.mapper.ParticipantMapper;
import com.konrad.konradquiz.mapper.QuestionMapper;
import com.konrad.konradquiz.repository.ParticipantRepository;
import com.konrad.konradquiz.service.interfaces.IParticipantService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import com.konrad.konradquiz.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements IParticipantService {

    private final ParticipantRepository participantRepository;
    private final IQuestionService questionService;
    private final ParticipantMapper participantMapper;
    private final QuestionMapper questionMapper;
    private final EncryptionUtil encryptionUtil;  // ← injected here only
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public ExperimentSessionDto register(ParticipantRequestDto dto) {

        String emailHash = encryptionUtil.hash(dto.getEmail());
        if (participantRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("A participant with this email already exists.");
        }

        Participant.ExperimentGroup group = SECURE_RANDOM.nextBoolean()
                ? Participant.ExperimentGroup.GROUP_A
                : Participant.ExperimentGroup.GROUP_B;

        Participant participant = Participant.builder()
                .alias(dto.getAlias())
                .email(dto.getEmail())
                .emailHash(emailHash)
                .sex(dto.getSex())
                .age(dto.getAge())
                .region(dto.getRegion())
                .experimentGroup(group)
                .build();

        Participant saved = participantRepository.save(participant); // ← capture saved

        List<Question> allQuestions = new ArrayList<>(questionService.getAllForSession());
        Collections.shuffle(allQuestions);

        List<QuestionSessionDto> sessionQuestions = new ArrayList<>();
        for (int i = 0; i < allQuestions.size(); i++) {
            sessionQuestions.add(questionMapper.toSessionDto(allQuestions.get(i), i + 1));
        }

        return ExperimentSessionDto.builder()
                .participantId(saved.getId())
                .alias(saved.getAlias())
                .experimentGroup(saved.getExperimentGroup())
                .questions(sessionQuestions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponseDto findById(Long id) {
        return participantRepository.findById(id)
                .map(participantMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Participant findEntityById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + id));
    }

    @Override
    @Transactional
    public void recordCompletionTime(Long participantId, Long completionTimeSeconds) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));
        participant.setCompletionTimeSeconds(completionTimeSeconds);
        participantRepository.save(participant);
    }
}
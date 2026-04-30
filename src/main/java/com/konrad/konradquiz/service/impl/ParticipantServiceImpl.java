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

    // Remove SecureRandom, inject GroupAssignmentService instead
    private final GroupAssignmentService groupAssignmentService;
    private final SessionBuilderService sessionBuilderService;
    @Override
    public ExperimentSessionDto register(ParticipantRequestDto dto) {

        // ── Consent validation ───────────────────────────────────────────────
        validateConsents(dto);

        // ── Duplicate check ──────────────────────────────────────────────────
        String emailHash = encryptionUtil.hash(dto.getEmail());
        if (participantRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("A participant with this email already exists.");
        }

        // ── Group + set assignment ───────────────────────────────────────────
        GroupAssignmentService.AssignedGroup assignment = groupAssignmentService.assignGroup();
        Question.NewsSet newsSet = groupAssignmentService.assignNewsSet();

        // ── Save participant ─────────────────────────────────────────────────
        Participant participant = Participant.builder()
                .alias(dto.getAlias())
                .email(dto.getEmail())
                .emailHash(emailHash)
                .sex(dto.getSex())
                .age(dto.getAge())
                .region(dto.getRegion())
                .feedbackTiming(assignment.feedbackTiming())
                .presentationFormat(assignment.presentationFormat())
                .newsSet(newsSet)
                .consentAcademicPurpose(dto.getConsentAcademicPurpose())
                .consentParticipationProcess(dto.getConsentParticipationProcess())
                .consentDataProcessing(dto.getConsentDataProcessing())
                .consentNoRisk(dto.getConsentNoRisk())
                .consentNoPayment(dto.getConsentNoPayment())
                .consentProjectInfo(dto.getConsentProjectInfo())
                .build();

        Participant saved = participantRepository.save(participant);

        // ── Build session ────────────────────────────────────────────────────
        List<Question> profileQuestions = questionService.getAllProfileQuestions();
        List<Question> newsQuestions    = questionService.getNewsQuestionsForSet(newsSet);

        SessionBuilderService.BuiltSession session = sessionBuilderService.build(
                profileQuestions,
                newsQuestions,
                saved.getPresentationFormat()
        );

        return ExperimentSessionDto.builder()
                .participantId(saved.getId())
                .alias(saved.getAlias())
                .feedbackTiming(saved.getFeedbackTiming())
                .presentationFormat(saved.getPresentationFormat())
                .newsSet(newsSet)
                .profileQuestions(session.profileQuestions())
                .newsPart1Questions(session.newsPart1Questions())
                .newsPart2Questions(session.newsPart2Questions())
                .build();
    }

    private void validateConsents(ParticipantRequestDto dto) {
        if (!Boolean.TRUE.equals(dto.getConsentAcademicPurpose())      ||
                !Boolean.TRUE.equals(dto.getConsentParticipationProcess()) ||
                !Boolean.TRUE.equals(dto.getConsentDataProcessing())       ||
                !Boolean.TRUE.equals(dto.getConsentNoRisk())               ||
                !Boolean.TRUE.equals(dto.getConsentNoPayment())            ||
                !Boolean.TRUE.equals(dto.getConsentProjectInfo())) {
            throw new BusinessException(
                    "All consent declarations must be accepted to participate."
            );
        }
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
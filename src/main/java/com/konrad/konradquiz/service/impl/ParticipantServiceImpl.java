package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.BusinessException;
import com.konrad.konradquiz.exception.ResourceNotFoundException;
import com.konrad.konradquiz.mapper.ParticipantMapper;
import com.konrad.konradquiz.repository.ParticipantRepository;
import com.konrad.konradquiz.service.interfaces.IParticipantService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import com.konrad.konradquiz.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements IParticipantService {

    private final ParticipantRepository participantRepository;
    private final IQuestionService questionService;
    private final ParticipantMapper participantMapper;
    private final EncryptionUtil encryptionUtil;
    private final GroupAssignmentService groupAssignmentService;
    private final SessionBuilderService sessionBuilderService;

    @Override
    public ExperimentSessionDto register(ParticipantRequestDto dto) {
        // Fail fast on consent before doing the (deliberately slow) email hashing
        validateConsents(dto);

        String emailHash = encryptionUtil.hash(dto.getEmail());
        if (participantRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("A participant with this email already exists.");
        }

        GroupAssignmentService.AssignedGroup assignment = groupAssignmentService.assignGroup();
        Question.NewsSet newsSet = groupAssignmentService.assignNewsSet();

        Participant saved = participantRepository.save(newParticipant(dto, emailHash, assignment, newsSet));

        SessionBuilderService.BuiltSession session = sessionBuilderService.build(
                questionService.getAllProfileQuestions(),
                questionService.getNewsQuestionsForSet(newsSet),
                saved.getPresentationFormat());

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

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponseDto findById(Long id) {
        return participantMapper.toResponseDto(getParticipantOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Participant findEntityById(Long id) {
        return getParticipantOrThrow(id);
    }

    @Override
    public void recordCompletionTime(Long participantId, Long completionTimeSeconds) {
        Participant participant = getParticipantOrThrow(participantId);
        participant.setCompletionTimeSeconds(completionTimeSeconds);
        participantRepository.save(participant);
    }

    private Participant getParticipantOrThrow(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + id));
    }

    private static void validateConsents(ParticipantRequestDto dto) {
        boolean allAccepted = Stream.of(
                        dto.getConsentAcademicPurpose(),
                        dto.getConsentParticipationProcess(),
                        dto.getConsentDataProcessing(),
                        dto.getConsentNoRisk(),
                        dto.getConsentNoPayment(),
                        dto.getConsentProjectInfo())
                .allMatch(Boolean.TRUE::equals);
        if (!allAccepted) {
            throw new BusinessException("All consent declarations must be accepted to participate.");
        }
    }

    private static Participant newParticipant(ParticipantRequestDto dto, String emailHash,
                                              GroupAssignmentService.AssignedGroup assignment,
                                              Question.NewsSet newsSet) {
        return Participant.builder()
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
    }
}

package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.request.RewardRequestDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.dto.response.RewardResponseDto;
import com.konrad.konradquiz.entity.Participant;
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

    /**
     * Registers the participant and builds their session. Group assignment and the save run in this same
     * transaction: {@link GroupAssignmentService#assign()} holds a SQL Server application lock until commit.
     */
    @Override
    public ExperimentSessionDto register(ParticipantRequestDto dto) {
        // Fail fast on consent before doing the (deliberately slow) email hashing
        validateConsents(dto);

        String emailHash = encryptionUtil.hash(dto.getEmail());
        if (participantRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("A participant with this email already exists.");
        }

        GroupAssignmentService.AssignedGroup assignment = groupAssignmentService.assign();
        Participant saved = participantRepository.save(newParticipant(dto, emailHash, assignment));

        SessionBuilderService.BuiltSession session = sessionBuilderService.build(
                questionService.getAllProfileQuestions(),
                questionService.getNewsQuestionsForSet(assignment.newsSet()),
                saved.getPresentationFormat());

        return ExperimentSessionDto.builder()
                .participantId(saved.getId())
                .alias(saved.getAlias())
                .feedbackTiming(saved.getFeedbackTiming())
                .presentationFormat(saved.getPresentationFormat())
                .newsSet(assignment.newsSet())
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

    /** A reward can be chosen once, and only after the answers were submitted (completion time recorded). */
    @Override
    public RewardResponseDto selectReward(Long participantId, RewardRequestDto dto) {
        Participant participant = getParticipantOrThrow(participantId);

        if (participant.getCompletionTimeSeconds() == null) {
            throw new BusinessException("Participant has not completed the experiment yet.");
        }
        if (participant.getRewardId() != null) {
            throw new BusinessException("Reward has already been selected for this participant.");
        }

        participant.setRewardId(dto.getRewardId());
        participantRepository.save(participant);

        return RewardResponseDto.builder()
                .participantId(participantId)
                .rewardId(dto.getRewardId())
                .message("Reward " + dto.getRewardId() + " selected successfully.")
                .build();
    }

    private Participant getParticipantOrThrow(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + id));
    }

    /** consentProjectInfo is optional; every other declaration must be accepted. */
    private static void validateConsents(ParticipantRequestDto dto) {
        boolean allRequiredAccepted = Stream.of(
                        dto.getConsentAcademicPurpose(),
                        dto.getConsentParticipationProcess(),
                        dto.getConsentDataProcessing(),
                        dto.getConsentNoRisk(),
                        dto.getConsentNoPayment())
                .allMatch(Boolean.TRUE::equals);
        if (!allRequiredAccepted) {
            throw new BusinessException("All required consent declarations must be accepted to participate.");
        }
    }

    private static Participant newParticipant(ParticipantRequestDto dto, String emailHash,
                                              GroupAssignmentService.AssignedGroup assignment) {
        return Participant.builder()
                .alias(dto.getAlias())
                .email(dto.getEmail())
                .emailHash(emailHash)
                .sex(dto.getSex())
                .age(dto.getAge())
                .region(dto.getRegion())
                .feedbackTiming(assignment.feedbackTiming())
                .presentationFormat(assignment.presentationFormat())
                .newsSet(assignment.newsSet())
                .consentAcademicPurpose(dto.getConsentAcademicPurpose())
                .consentParticipationProcess(dto.getConsentParticipationProcess())
                .consentDataProcessing(dto.getConsentDataProcessing())
                .consentNoRisk(dto.getConsentNoRisk())
                .consentNoPayment(dto.getConsentNoPayment())
                .consentProjectInfo(dto.getConsentProjectInfo())
                .build();
    }
}

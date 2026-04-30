package com.konrad.konradquiz.service;

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
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import com.konrad.konradquiz.service.impl.ParticipantServiceImpl;
import com.konrad.konradquiz.service.impl.SessionBuilderService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import com.konrad.konradquiz.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService Tests")
class ParticipantServiceTest {

    @Mock private ParticipantRepository participantRepository;
    @Mock private IQuestionService questionService;
    @Mock private ParticipantMapper participantMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private EncryptionUtil encryptionUtil;
    @Mock private GroupAssignmentService groupAssignmentService;
    @Mock private SessionBuilderService sessionBuilderService;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    private ParticipantRequestDto validRequest;
    private Participant savedParticipant;
    private List<Question> mockProfileQuestions;
    private List<Question> mockNewsQuestions;

    @BeforeEach
    void setUp() {
        validRequest = new ParticipantRequestDto();
        validRequest.setAlias("testuser");
        validRequest.setEmail("test@email.com");
        validRequest.setSex(Participant.Sex.MALE);
        validRequest.setAge(25);
        validRequest.setRegion("Bogota");
        // All consents true
        validRequest.setConsentAcademicPurpose(true);
        validRequest.setConsentParticipationProcess(true);
        validRequest.setConsentDataProcessing(true);
        validRequest.setConsentNoRisk(true);
        validRequest.setConsentNoPayment(true);
        validRequest.setConsentProjectInfo(true);

        savedParticipant = Participant.builder()
                .id(1L)
                .alias("testuser")
                .email("encrypted_email")
                .emailHash("hashed_email")
                .sex(Participant.Sex.MALE)
                .age(25)
                .region("Bogota")
                .feedbackTiming(Participant.FeedbackTiming.GROUP_A)
                .presentationFormat(Participant.PresentationFormat.WHATSAPP)
                .newsSet(Question.NewsSet.TECHNOLOGY)
                .registeredAt(LocalDateTime.now())
                .build();

        mockProfileQuestions = List.of(
                buildQuestion("P1", Question.QuestionType.PROFILE),
                buildQuestion("P2", Question.QuestionType.PROFILE)
        );

        mockNewsQuestions = List.of(
                buildQuestion("1_exp1", Question.QuestionType.NEWS),
                buildQuestion("2_exp1", Question.QuestionType.NEWS),
                buildQuestion("3_exp1", Question.QuestionType.NEWS),
                buildQuestion("4_exp1", Question.QuestionType.NEWS),
                buildQuestion("5_exp1", Question.QuestionType.NEWS),
                buildQuestion("6_exp1", Question.QuestionType.NEWS),
                buildQuestion("7_exp1", Question.QuestionType.NEWS),
                buildQuestion("8_exp1", Question.QuestionType.NEWS),
                buildQuestion("9_exp1", Question.QuestionType.NEWS),
                buildQuestion("10_exp1", Question.QuestionType.NEWS),
                buildQuestion("11_exp1", Question.QuestionType.NEWS),
                buildQuestion("12_exp1", Question.QuestionType.NEWS)
        );
    }

    // ── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() — success: returns session with 3 question lists")
    void register_success() {
        // Arrange
        mockSuccessfulRegistration();

        // Act
        ExperimentSessionDto result = participantService.register(validRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1L);
        assertThat(result.getAlias()).isEqualTo("testuser");
        assertThat(result.getFeedbackTiming()).isEqualTo(Participant.FeedbackTiming.GROUP_A);
        assertThat(result.getPresentationFormat()).isEqualTo(Participant.PresentationFormat.WHATSAPP);
        assertThat(result.getNewsSet()).isEqualTo(Question.NewsSet.TECHNOLOGY);
        assertThat(result.getProfileQuestions()).isNotNull();
        assertThat(result.getNewsPart1Questions()).isNotNull();
        assertThat(result.getNewsPart2Questions()).isNotNull();

        verify(participantRepository).save(any(Participant.class));
        verify(sessionBuilderService).build(any(), any(), any());
    }

    @Test
    @DisplayName("register() — throws BusinessException when any consent is false")
    void register_consentFalse_throwsBusinessException() {
        // Arrange — one consent is false
        validRequest.setConsentDataProcessing(false);

        // Act & Assert
        assertThatThrownBy(() -> participantService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("consent");

        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() — throws BusinessException when all consents are false")
    void register_allConsentsFalse_throwsBusinessException() {
        // Arrange
        validRequest.setConsentAcademicPurpose(false);
        validRequest.setConsentParticipationProcess(false);
        validRequest.setConsentDataProcessing(false);
        validRequest.setConsentNoRisk(false);
        validRequest.setConsentNoPayment(false);
        validRequest.setConsentProjectInfo(false);

        // Act & Assert
        assertThatThrownBy(() -> participantService.register(validRequest))
                .isInstanceOf(BusinessException.class);

        verify(participantRepository, never()).save(any());
        verify(encryptionUtil, never()).hash(any());
    }

    @Test
    @DisplayName("register() — throws BusinessException when email already exists")
    void register_duplicateEmail_throwsBusinessException() {
        // Arrange
        when(encryptionUtil.hash(validRequest.getEmail())).thenReturn("hashed_email");
        when(participantRepository.existsByEmailHash("hashed_email")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> participantService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(participantRepository, never()).save(any());
        verify(questionService, never()).getAllProfileQuestions();
    }

    @Test
    @DisplayName("register() — group assignment called exactly once")
    void register_groupAssignmentCalledOnce() {
        // Arrange
        mockSuccessfulRegistration();

        // Act
        participantService.register(validRequest);

        // Assert
        verify(groupAssignmentService, times(1)).assignGroup();
        verify(groupAssignmentService, times(1)).assignNewsSet();
    }

    @Test
    @DisplayName("register() — consent is validated before email hash (fail fast)")
    void register_consentValidatedBeforeEmailHash() {
        // Arrange
        validRequest.setConsentNoRisk(false);

        // Act & Assert
        assertThatThrownBy(() -> participantService.register(validRequest))
                .isInstanceOf(BusinessException.class);

        // Should never reach email hashing
        verify(encryptionUtil, never()).hash(any());
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() — success: returns mapped DTO")
    void findById_success() {
        ParticipantResponseDto expectedDto = ParticipantResponseDto.builder()
                .id(1L)
                .alias("testuser")
                .build();

        when(participantRepository.findById(1L)).thenReturn(Optional.of(savedParticipant));
        when(participantMapper.toResponseDto(savedParticipant)).thenReturn(expectedDto);

        ParticipantResponseDto result = participantService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAlias()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findById() — throws ResourceNotFoundException when not found")
    void findById_notFound_throwsResourceNotFoundException() {
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── findEntityById() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findEntityById() — returns raw entity for internal use")
    void findEntityById_success() {
        when(participantRepository.findById(1L)).thenReturn(Optional.of(savedParticipant));

        Participant result = participantService.findEntityById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findEntityById() — throws ResourceNotFoundException when not found")
    void findEntityById_notFound_throwsResourceNotFoundException() {
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.findEntityById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockSuccessfulRegistration() {
        when(encryptionUtil.hash(validRequest.getEmail())).thenReturn("hashed_email");
        when(participantRepository.existsByEmailHash("hashed_email")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenReturn(savedParticipant);
        when(groupAssignmentService.assignGroup())
                .thenReturn(new GroupAssignmentService.AssignedGroup(
                        Participant.FeedbackTiming.GROUP_A,
                        Participant.PresentationFormat.WHATSAPP
                ));
        when(groupAssignmentService.assignNewsSet())
                .thenReturn(Question.NewsSet.TECHNOLOGY);
        when(questionService.getAllProfileQuestions()).thenReturn(mockProfileQuestions);
        when(questionService.getNewsQuestionsForSet(Question.NewsSet.TECHNOLOGY))
                .thenReturn(mockNewsQuestions);
        when(sessionBuilderService.build(any(), any(), any()))
                .thenReturn(new SessionBuilderService.BuiltSession(
                        List.of(QuestionSessionDto.builder().questionCode("P1").build()),
                        List.of(QuestionSessionDto.builder().questionCode("1_exp1").build()),
                        List.of(QuestionSessionDto.builder().questionCode("9_exp1").build())
                ));
    }

    private Question buildQuestion(String code, Question.QuestionType type) {
        return Question.builder()
                .questionCode(code)
                .constructo("Creador")
                .itemText("Test question " + code)
                .questionType(type)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();
    }
}
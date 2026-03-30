package com.konrad.konradquiz.service;

import com.konrad.konradquiz.dto.request.ParticipantRequestDto;
import com.konrad.konradquiz.dto.response.ExperimentSessionDto;
import com.konrad.konradquiz.dto.response.ParticipantResponseDto;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.BusinessException;
import com.konrad.konradquiz.exception.ResourceNotFoundException;
import com.konrad.konradquiz.mapper.ParticipantMapper;
import com.konrad.konradquiz.mapper.QuestionMapper;
import com.konrad.konradquiz.repository.ParticipantRepository;
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import com.konrad.konradquiz.service.impl.ParticipantServiceImpl;
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

    @InjectMocks
    private ParticipantServiceImpl participantService;

    // ── Test data ────────────────────────────────────────────────────────────

    private ParticipantRequestDto validRequest;
    private Participant savedParticipant;
    private List<Question> mockQuestions;

    @BeforeEach
    void setUp() {
        validRequest = new ParticipantRequestDto();
        validRequest.setAlias("testuser");
        validRequest.setEmail("test@email.com");
        validRequest.setSex(Participant.Sex.MALE);
        validRequest.setAge(25);

        savedParticipant = Participant.builder()
                .id(1L)
                .alias("testuser")
                .email("encrypted_email")
                .emailHash("hashed_email")
                .sex(Participant.Sex.MALE)
                .age(25)
                .feedbackTiming(Participant.FeedbackTiming.GROUP_A)
                .registeredAt(LocalDateTime.now())
                .build();

        mockQuestions = List.of(
                buildQuestion("P1", "Creador"),
                buildQuestion("P2", "Contenido"),
                buildQuestion("P3", "Víctimas"),
                buildQuestion("P4", "Contexto")
        );
    }

    // ── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() — success: returns session with shuffled questions")
    void register_success() {
        // Arrange
        when(encryptionUtil.hash(validRequest.getEmail())).thenReturn("hashed_email");
        when(participantRepository.existsByEmailHash("hashed_email")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenReturn(savedParticipant);
        when(questionService.getAllForSession()).thenReturn(mockQuestions);
        when(questionMapper.toSessionDto(any(Question.class), anyInt()))
                .thenAnswer(inv -> {
                    Question q = inv.getArgument(0);
                    int order = inv.getArgument(1);
                    return com.konrad.konradquiz.dto.response.QuestionSessionDto.builder()
                            .questionCode(q.getQuestionCode())
                            .assignedOrder(order)
                            .build();
                });
        when(groupAssignmentService.assignGroup())
                .thenReturn(new GroupAssignmentService.AssignedGroup(
                        Participant.FeedbackTiming.GROUP_A,
                        Participant.PresentationFormat.INSTAGRAM
                ));

        // Act
        ExperimentSessionDto result = participantService.register(validRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1L);
        assertThat(result.getAlias()).isEqualTo("testuser");
        assertThat(result.getQuestions()).hasSize(4);
        assertThat(result.getFeedbackTiming()).isIn(
                Participant.FeedbackTiming.GROUP_A,
                Participant.FeedbackTiming.GROUP_B
        );
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

        // Verify we never reached save
        verify(participantRepository, never()).save(any());
        verify(questionService, never()).getAllForSession();
    }

    @Test
    @DisplayName("register() — group is always GROUP_A or GROUP_B")
    void register_groupAssignment_isAlwaysValid() {
        // Arrange
        when(encryptionUtil.hash(anyString())).thenReturn("hashed_email");
        when(participantRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(participantRepository.save(any())).thenReturn(savedParticipant);
        when(questionService.getAllForSession()).thenReturn(mockQuestions);
        when(questionMapper.toSessionDto(any(), anyInt()))
                .thenReturn(com.konrad.konradquiz.dto.response.QuestionSessionDto.builder()
                        .questionCode("P1").assignedOrder(1).build());

        // Alternar entre GROUP_A y GROUP_B en cada llamada
        when(groupAssignmentService.assignGroup())
                .thenReturn(new GroupAssignmentService.AssignedGroup(
                        Participant.FeedbackTiming.GROUP_A,
                        Participant.PresentationFormat.INSTAGRAM
                ))
                .thenReturn(new GroupAssignmentService.AssignedGroup(
                        Participant.FeedbackTiming.GROUP_B,
                        Participant.PresentationFormat.WHATSAPP
                ));

        // Act — run 20 times
        for (int i = 0; i < 20; i++) {
            ExperimentSessionDto result = participantService.register(validRequest);
            assertThat(result.getFeedbackTiming())
                    .isIn(Participant.FeedbackTiming.GROUP_A, Participant.FeedbackTiming.GROUP_B);
        }
    }


    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() — success: returns mapped DTO")
    void findById_success() {
        // Arrange
        ParticipantResponseDto expectedDto = ParticipantResponseDto.builder()
                .id(1L)
                .alias("testuser")
                .build();

        when(participantRepository.findById(1L)).thenReturn(Optional.of(savedParticipant));
        when(participantMapper.toResponseDto(savedParticipant)).thenReturn(expectedDto);

        // Act
        ParticipantResponseDto result = participantService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAlias()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findById() — throws ResourceNotFoundException when participant not found")
    void findById_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
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

    private Question buildQuestion(String code, String constructo) {
        return Question.builder()
                .questionCode(code)
                .constructo(constructo)
                .itemText("Test question " + code)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();
    }
}
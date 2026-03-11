package com.konrad.konradquiz.service;

import com.konrad.konradquiz.dto.request.AnswerRequestDto;
import com.konrad.konradquiz.dto.request.BatchAnswerRequestDto;
import com.konrad.konradquiz.dto.response.BatchAnswerResponseDto;
import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.BusinessException;
import com.konrad.konradquiz.repository.AnswerRepository;
import com.konrad.konradquiz.service.impl.AnswerServiceImpl;
import com.konrad.konradquiz.service.interfaces.IParticipantService;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerService Tests")
class AnswerServiceTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private IParticipantService participantService;
    @Mock private IQuestionService questionService;

    @InjectMocks
    private AnswerServiceImpl answerService;

    private Participant mockParticipant;
    private BatchAnswerRequestDto validBatchRequest;

    @BeforeEach
    void setUp() {
        mockParticipant = Participant.builder()
                .id(1L)
                .alias("testuser")
                .experimentGroup(Participant.ExperimentGroup.GROUP_A)
                .build();

        // Build 4 valid answers matching 4 mock questions
        validBatchRequest = new BatchAnswerRequestDto();
        validBatchRequest.setAnswers(List.of(
                buildAnswerRequest("P1", 75, 1),
                buildAnswerRequest("P2", 20, 2),
                buildAnswerRequest("P3", 90, 3),
                buildAnswerRequest("P4", 45, 4)
        ));
    }

    // ── submitBatch() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitBatch() — success: saves all answers and returns count")
    void submitBatch_success() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countAll()).thenReturn(4L);
        when(questionService.findEntityByCode(anyString()))
                .thenAnswer(inv -> buildQuestion(inv.getArgument(0)));
        when(answerRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        BatchAnswerResponseDto result = answerService.submitBatch(1L, validBatchRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1L);
        assertThat(result.getSavedCount()).isEqualTo(4);

        // Verify saveAll called once with correct number of answers
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(4);
    }

    @Test
    @DisplayName("submitBatch() — scores are stored correctly per question")
    void submitBatch_scoresStoredCorrectly() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countAll()).thenReturn(4L);
        when(questionService.findEntityByCode(anyString()))
                .thenAnswer(inv -> buildQuestion(inv.getArgument(0)));
        when(answerRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert — capture and verify scores
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<Answer> savedAnswers = captor.getValue();
        assertThat(savedAnswers.get(0).getScore()).isEqualTo(75);
        assertThat(savedAnswers.get(1).getScore()).isEqualTo(20);
        assertThat(savedAnswers.get(2).getScore()).isEqualTo(90);
        assertThat(savedAnswers.get(3).getScore()).isEqualTo(45);
    }

    @Test
    @DisplayName("submitBatch() — question order is stored correctly")
    void submitBatch_questionOrderStoredCorrectly() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countAll()).thenReturn(4L);
        when(questionService.findEntityByCode(anyString()))
                .thenAnswer(inv -> buildQuestion(inv.getArgument(0)));
        when(answerRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<Answer> savedAnswers = captor.getValue();
        assertThat(savedAnswers.get(0).getQuestionOrder()).isEqualTo(1);
        assertThat(savedAnswers.get(1).getQuestionOrder()).isEqualTo(2);
        assertThat(savedAnswers.get(2).getQuestionOrder()).isEqualTo(3);
        assertThat(savedAnswers.get(3).getQuestionOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("submitBatch() — throws BusinessException when participant already submitted")
    void submitBatch_alreadySubmitted_throwsBusinessException() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(4L); // already has answers

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, validBatchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already submitted");

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitBatch() — throws BusinessException when answer count doesn't match questions")
    void submitBatch_wrongAnswerCount_throwsBusinessException() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countAll()).thenReturn(17L); // 17 questions but only 4 answers sent

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, validBatchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("17")
                .hasMessageContaining("4");

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitBatch() — saveAll called once (single DB round trip)")
    void submitBatch_singleDbRoundTrip() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countAll()).thenReturn(4L);
        when(questionService.findEntityByCode(anyString()))
                .thenAnswer(inv -> buildQuestion(inv.getArgument(0)));
        when(answerRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert — saveAll called exactly once, not per answer
        verify(answerRepository, times(1)).saveAll(anyList());
        verify(answerRepository, never()).save(any()); // never called individually
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AnswerRequestDto buildAnswerRequest(String code, int score, int order) {
        AnswerRequestDto dto = new AnswerRequestDto();
        dto.setQuestionCode(code);
        dto.setScore(score);
        dto.setQuestionOrder(order);
        return dto;
    }

    private Question buildQuestion(String code) {
        return Question.builder()
                .questionCode(code)
                .constructo("Creador")
                .itemText("Test question " + code)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();
    }
}
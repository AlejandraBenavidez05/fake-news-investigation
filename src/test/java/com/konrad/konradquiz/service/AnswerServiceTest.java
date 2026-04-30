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
import com.konrad.konradquiz.service.impl.SessionBuilderService;
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

import java.time.Instant;
import java.util.ArrayList;
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

    // Total = 18 profile + 8 part1 + 8 part2 = 34
    private static final int PROFILE_COUNT = 18;

    @BeforeEach
    void setUp() {
        mockParticipant = Participant.builder()
                .id(1L)
                .alias("testuser")
                .feedbackTiming(Participant.FeedbackTiming.GROUP_A)
                .build();

        validBatchRequest = new BatchAnswerRequestDto();
        validBatchRequest.setStartedAt(Instant.now().getEpochSecond() - 30);
        validBatchRequest.setAnswers(buildFullAnswerSet());
    }

    // ── submitBatch() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitBatch() — success: saves all 34 answers and returns count")
    void submitBatch_success() {
        // Arrange
        mockSuccessfulSubmit();

        // Act
        BatchAnswerResponseDto result = answerService.submitBatch(1L, validBatchRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1L);
        assertThat(result.getSavedCount()).isEqualTo(34);

        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(34);
    }

    @Test
    @DisplayName("submitBatch() — answerType is stored correctly per answer")
    void submitBatch_answerTypeStoredCorrectly() {
        // Arrange
        mockSuccessfulSubmit();

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<Answer> saved = captor.getValue();

        long profileCount = saved.stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.PROFILE).count();
        long part1Count = saved.stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.FAKE_DETECTION).count();
        long part2Count = saved.stream()
                .filter(a -> a.getAnswerType() == Answer.AnswerType.MEMORY_TEST).count();

        assertThat(profileCount).isEqualTo(PROFILE_COUNT);
        assertThat(part1Count).isEqualTo(SessionBuilderService.PART1_SIZE);
        assertThat(part2Count).isEqualTo(SessionBuilderService.PART2_SIZE);
    }

    @Test
    @DisplayName("submitBatch() — scores are stored correctly per question")
    void submitBatch_scoresStoredCorrectly() {
        // Arrange
        mockSuccessfulSubmit();

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());
        verify(participantService).recordCompletionTime(eq(1L), anyLong());

        List<Answer> saved = captor.getValue();
        // First profile answer score is 33
        assertThat(saved.get(0).getScore()).isEqualTo(33);
        // First news part1 answer score is 75
        assertThat(saved.get(PROFILE_COUNT).getScore()).isEqualTo(75);
        // First news part2 answer score is 80
        assertThat(saved.get(PROFILE_COUNT + SessionBuilderService.PART1_SIZE).getScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("submitBatch() — question order is stored correctly")
    void submitBatch_questionOrderStoredCorrectly() {
        // Arrange
        mockSuccessfulSubmit();

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        ArgumentCaptor<List<Answer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());
        verify(participantService).recordCompletionTime(eq(1L), anyLong());

        List<Answer> saved = captor.getValue();
        assertThat(saved.get(0).getQuestionOrder()).isEqualTo(1);
        assertThat(saved.get(1).getQuestionOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("submitBatch() — throws BusinessException when participant already submitted")
    void submitBatch_alreadySubmitted_throwsBusinessException() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(34L);

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, validBatchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already submitted");

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitBatch() — throws BusinessException when profile count is wrong")
    void submitBatch_wrongProfileCount_throwsBusinessException() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countByType(Question.QuestionType.PROFILE)).thenReturn(20L); // expects 20, sent 18

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, validBatchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("profile");

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitBatch() — throws BusinessException when part1 count is wrong")
    void submitBatch_wrongPart1Count_throwsBusinessException() {
        // Arrange — send only 5 FAKE_DETECTION instead of 8
        BatchAnswerRequestDto badRequest = new BatchAnswerRequestDto();
        badRequest.setStartedAt(Instant.now().getEpochSecond() - 30);
        List<AnswerRequestDto> answers = new ArrayList<>(buildProfileAnswers());
        answers.addAll(buildNewsAnswers(5, Answer.AnswerType.FAKE_DETECTION, "N_P1_"));
        answers.addAll(buildNewsAnswers(SessionBuilderService.PART2_SIZE, Answer.AnswerType.MEMORY_TEST, "N_P2_"));
        badRequest.setAnswers(answers);

        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countByType(Question.QuestionType.PROFILE)).thenReturn((long) PROFILE_COUNT);

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, badRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Part 1");

        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("submitBatch() — saveAll called once (single DB round trip)")
    void submitBatch_singleDbRoundTrip() {
        // Arrange
        mockSuccessfulSubmit();

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        verify(answerRepository, times(1)).saveAll(anyList());
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitBatch() — completion time is recorded after saving answers")
    void submitBatch_completionTimeRecorded() {
        // Arrange
        long startedAt = Instant.now().getEpochSecond() - 45;
        validBatchRequest.setStartedAt(startedAt);
        mockSuccessfulSubmit();

        // Act
        answerService.submitBatch(1L, validBatchRequest);

        // Assert
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(participantService).recordCompletionTime(eq(1L), timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThanOrEqualTo(44L);
        assertThat(timeCaptor.getValue()).isLessThan(60L);
    }

    @Test
    @DisplayName("submitBatch() — completion time not recorded when already submitted")
    void submitBatch_alreadySubmitted_completionTimeNotRecorded() {
        // Arrange
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(34L);

        // Act & Assert
        assertThatThrownBy(() -> answerService.submitBatch(1L, validBatchRequest))
                .isInstanceOf(BusinessException.class);

        verify(participantService, never()).recordCompletionTime(any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockSuccessfulSubmit() {
        when(participantService.findEntityById(1L)).thenReturn(mockParticipant);
        when(answerRepository.countByParticipantId(1L)).thenReturn(0L);
        when(questionService.countByType(Question.QuestionType.PROFILE))
                .thenReturn((long) PROFILE_COUNT);
        when(questionService.findEntityByCode(anyString()))
                .thenAnswer(inv -> buildQuestion(inv.getArgument(0)));
        when(answerRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private List<AnswerRequestDto> buildFullAnswerSet() {
        List<AnswerRequestDto> answers = new ArrayList<>();
        answers.addAll(buildProfileAnswers());
        answers.addAll(buildNewsAnswers(SessionBuilderService.PART1_SIZE,
                Answer.AnswerType.FAKE_DETECTION, "N_P1_"));
        answers.addAll(buildNewsAnswers(SessionBuilderService.PART2_SIZE,
                Answer.AnswerType.MEMORY_TEST, "N_P2_"));
        return answers;
    }

    private List<AnswerRequestDto> buildProfileAnswers() {
        List<AnswerRequestDto> answers = new ArrayList<>();
        for (int i = 1; i <= PROFILE_COUNT; i++) {
            AnswerRequestDto dto = new AnswerRequestDto();
            dto.setQuestionCode("P" + i);
            dto.setScore(33);
            dto.setQuestionOrder(i);
            dto.setAnswerType(Answer.AnswerType.PROFILE);
            answers.add(dto);
        }
        return answers;
    }

    private List<AnswerRequestDto> buildNewsAnswers(int count, Answer.AnswerType type, String prefix) {
        List<AnswerRequestDto> answers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            AnswerRequestDto dto = new AnswerRequestDto();
            dto.setQuestionCode(prefix + i);
            dto.setScore(type == Answer.AnswerType.FAKE_DETECTION ? 75 : 80);
            dto.setQuestionOrder(i);
            dto.setAnswerType(type);
            answers.add(dto);
        }
        return answers;
    }

    private Question buildQuestion(String code) {
        return Question.builder()
                .questionCode(code)
                .constructo("Creador")
                .itemText("Test question " + code)
                .questionType(Question.QuestionType.NEWS)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();
    }
}
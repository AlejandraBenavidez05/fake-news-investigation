package com.konrad.konradquiz.service;

import com.konrad.konradquiz.dto.request.QuestionCorrectAnswerRequestDto;
import com.konrad.konradquiz.dto.response.QuestionAdminResponseDto;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.ResourceNotFoundException;
import com.konrad.konradquiz.mapper.QuestionMapper;
import com.konrad.konradquiz.repository.QuestionRepository;
import com.konrad.konradquiz.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService Tests")
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        mockQuestion = Question.builder()
                .questionCode("P1")
                .constructo("Creador")
                .subCategory("Humano Real")
                .subCategory2("Atributos de Fuente")
                .itemText("Valoro el contenido de una información por encima del prestigio")
                .correctAnswer(null) // not set yet — like real data
                .referenceApa("Zhang, X., & Ghorbani, A. A. (2020)")
                .supportingQuote("Users are more likely to trust and share news")
                .build();
    }

    // ── getAllForSession() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllForSession() — returns all questions from repository")
    void getAllForSession_returnsAllQuestions() {
        // Arrange
        List<Question> expected = List.of(
                mockQuestion,
                Question.builder().questionCode("P2").constructo("Contenido").itemText("Q2").build()
        );
        when(questionRepository.findAll()).thenReturn(expected);

        // Act
        List<Question> result = questionService.getAllForSession();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuestionCode()).isEqualTo("P1");
        assertThat(result.get(1).getQuestionCode()).isEqualTo("P2");
        verify(questionRepository).findAll();
    }

    @Test
    @DisplayName("getAllForSession() — returns empty list when no questions exist")
    void getAllForSession_empty() {
        when(questionRepository.findAll()).thenReturn(List.of());

        List<Question> result = questionService.getAllForSession();

        assertThat(result).isEmpty();
    }

    // ── findEntityByCode() ───────────────────────────────────────────────────

    @Test
    @DisplayName("findEntityByCode() — returns question entity when found")
    void findEntityByCode_success() {
        when(questionRepository.findById("P1")).thenReturn(Optional.of(mockQuestion));

        Question result = questionService.findEntityByCode("P1");

        assertThat(result).isNotNull();
        assertThat(result.getQuestionCode()).isEqualTo("P1");
        assertThat(result.getConstructo()).isEqualTo("Creador");
    }

    @Test
    @DisplayName("findEntityByCode() — throws ResourceNotFoundException when not found")
    void findEntityByCode_notFound_throwsResourceNotFoundException() {
        when(questionRepository.findById("P99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findEntityByCode("P99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P99");
    }

    // ── countAll() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("countAll() — returns correct question count")
    void countAll_returnsCorrectCount() {
        when(questionRepository.count()).thenReturn(17L);

        long result = questionService.countAll();

        assertThat(result).isEqualTo(17L);
    }

    // ── updateCorrectAnswer() ────────────────────────────────────────────────

    @Test
    @DisplayName("updateCorrectAnswer() — success: updates and returns admin DTO")
    void updateCorrectAnswer_success() {
        // Arrange
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.FAKE);

        Question updatedQuestion = Question.builder()
                .questionCode("P1")
                .itemText("Valoro el contenido...")
                .correctAnswer(Question.CorrectAnswer.FAKE)
                .build();

        when(questionRepository.findById("P1")).thenReturn(Optional.of(mockQuestion));
        when(questionRepository.save(any(Question.class))).thenReturn(updatedQuestion);

        // Act
        QuestionAdminResponseDto result = questionService.updateCorrectAnswer("P1", request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getQuestionCode()).isEqualTo("P1");
        assertThat(result.getCorrectAnswer()).isEqualTo(Question.CorrectAnswer.FAKE);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("updateCorrectAnswer() — throws ResourceNotFoundException when question not found")
    void updateCorrectAnswer_notFound_throwsResourceNotFoundException() {
        // Arrange
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.REAL);

        when(questionRepository.findById("P99")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.updateCorrectAnswer("P99", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P99");

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCorrectAnswer() — can set REAL")
    void updateCorrectAnswer_setReal() {
        // Arrange
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.REAL);

        Question updated = Question.builder()
                .questionCode("P1")
                .itemText("test")
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();

        when(questionRepository.findById("P1")).thenReturn(Optional.of(mockQuestion));
        when(questionRepository.save(any())).thenReturn(updated);

        // Act
        QuestionAdminResponseDto result = questionService.updateCorrectAnswer("P1", request);

        // Assert
        assertThat(result.getCorrectAnswer()).isEqualTo(Question.CorrectAnswer.REAL);
    }
}
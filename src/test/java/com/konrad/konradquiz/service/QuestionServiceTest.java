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

    private Question mockProfileQuestion;
    private Question mockNewsQuestion;

    @BeforeEach
    void setUp() {
        mockProfileQuestion = Question.builder()
                .questionCode("P1")
                .constructo("Creador")
                .subCategory("Humano Real")
                .subCategory2("Atributos de Fuente")
                .itemText("Valoro el contenido de una información por encima del prestigio")
                .questionType(Question.QuestionType.PROFILE)
                .correctAnswer(null)
                .referenceApa("Zhang, X., & Ghorbani, A. A. (2020)")
                .supportingQuote("Users are more likely to trust and share news")
                .scaleOptions("nada,poco,bastante,mucho")
                .build();

        mockNewsQuestion = Question.builder()
                .questionCode("1_exp1")
                .constructo("Fraude")
                .itemText("Alertan del fraude de instalar una app...")
                .questionType(Question.QuestionType.NEWS)
                .newsSet(Question.NewsSet.TECHNOLOGY)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .phase("DETECCION")
                .build();
    }

    // ── getAllProfileQuestions() ──────────────────────────────────────────────

    @Test
    @DisplayName("getAllProfileQuestions() — returns only PROFILE questions")
    void getAllProfileQuestions_returnsOnlyProfile() {
        when(questionRepository.findByQuestionType(Question.QuestionType.PROFILE))
                .thenReturn(List.of(mockProfileQuestion));

        List<Question> result = questionService.getAllProfileQuestions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestionType()).isEqualTo(Question.QuestionType.PROFILE);
        assertThat(result.get(0).getQuestionCode()).isEqualTo("P1");
        verify(questionRepository).findByQuestionType(Question.QuestionType.PROFILE);
    }

    @Test
    @DisplayName("getAllProfileQuestions() — returns empty list when none exist")
    void getAllProfileQuestions_empty() {
        when(questionRepository.findByQuestionType(Question.QuestionType.PROFILE))
                .thenReturn(List.of());

        List<Question> result = questionService.getAllProfileQuestions();

        assertThat(result).isEmpty();
    }

    // ── getNewsQuestionsForSet() ─────────────────────────────────────────────

    @Test
    @DisplayName("getNewsQuestionsForSet() — returns 12 questions for TECHNOLOGY set")
    void getNewsQuestionsForSet_technologyReturns12() {
        List<Question> tech12 = buildNewsList(12, Question.NewsSet.TECHNOLOGY);
        when(questionRepository.findByQuestionTypeAndNewsSet(
                Question.QuestionType.NEWS, Question.NewsSet.TECHNOLOGY))
                .thenReturn(tech12);

        List<Question> result = questionService.getNewsQuestionsForSet(Question.NewsSet.TECHNOLOGY);

        assertThat(result).hasSize(12);
        assertThat(result).allMatch(q -> q.getNewsSet() == Question.NewsSet.TECHNOLOGY);
    }

    @Test
    @DisplayName("getNewsQuestionsForSet() — returns 12 questions for ENVIRONMENT set")
    void getNewsQuestionsForSet_environmentReturns12() {
        List<Question> env12 = buildNewsList(12, Question.NewsSet.ENVIRONMENT);
        when(questionRepository.findByQuestionTypeAndNewsSet(
                Question.QuestionType.NEWS, Question.NewsSet.ENVIRONMENT))
                .thenReturn(env12);

        List<Question> result = questionService.getNewsQuestionsForSet(Question.NewsSet.ENVIRONMENT);

        assertThat(result).hasSize(12);
        assertThat(result).allMatch(q -> q.getNewsSet() == Question.NewsSet.ENVIRONMENT);
    }

    // ── findEntityByCode() ───────────────────────────────────────────────────

    @Test
    @DisplayName("findEntityByCode() — returns PROFILE question entity when found")
    void findEntityByCode_profileSuccess() {
        when(questionRepository.findById("P1")).thenReturn(Optional.of(mockProfileQuestion));

        Question result = questionService.findEntityByCode("P1");

        assertThat(result).isNotNull();
        assertThat(result.getQuestionCode()).isEqualTo("P1");
        assertThat(result.getQuestionType()).isEqualTo(Question.QuestionType.PROFILE);
    }

    @Test
    @DisplayName("findEntityByCode() — returns NEWS question entity when found")
    void findEntityByCode_newsSuccess() {
        when(questionRepository.findById("1_exp1")).thenReturn(Optional.of(mockNewsQuestion));

        Question result = questionService.findEntityByCode("1_exp1");

        assertThat(result).isNotNull();
        assertThat(result.getQuestionCode()).isEqualTo("1_exp1");
        assertThat(result.getQuestionType()).isEqualTo(Question.QuestionType.NEWS);
        assertThat(result.getNewsSet()).isEqualTo(Question.NewsSet.TECHNOLOGY);
    }

    @Test
    @DisplayName("findEntityByCode() — throws ResourceNotFoundException when not found")
    void findEntityByCode_notFound_throwsResourceNotFoundException() {
        when(questionRepository.findById("P99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findEntityByCode("P99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P99");
    }

    // ── countByType() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByType() — returns correct count for PROFILE")
    void countByType_profileCount() {
        when(questionRepository.countByQuestionType(Question.QuestionType.PROFILE))
                .thenReturn(18L);

        long result = questionService.countByType(Question.QuestionType.PROFILE);

        assertThat(result).isEqualTo(18L);
    }

    @Test
    @DisplayName("countByType() — returns correct count for NEWS")
    void countByType_newsCount() {
        when(questionRepository.countByQuestionType(Question.QuestionType.NEWS))
                .thenReturn(24L); // 12 env + 12 tech

        long result = questionService.countByType(Question.QuestionType.NEWS);

        assertThat(result).isEqualTo(24L);
    }

    // ── updateCorrectAnswer() ────────────────────────────────────────────────

    @Test
    @DisplayName("updateCorrectAnswer() — success: updates and returns admin DTO")
    void updateCorrectAnswer_success() {
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.FAKE);

        Question updated = Question.builder()
                .questionCode("P1")
                .itemText("test")
                .questionType(Question.QuestionType.PROFILE)
                .correctAnswer(Question.CorrectAnswer.FAKE)
                .build();

        when(questionRepository.findById("P1")).thenReturn(Optional.of(mockProfileQuestion));
        when(questionRepository.save(any(Question.class))).thenReturn(updated);

        QuestionAdminResponseDto result = questionService.updateCorrectAnswer("P1", request);

        assertThat(result).isNotNull();
        assertThat(result.getQuestionCode()).isEqualTo("P1");
        assertThat(result.getCorrectAnswer()).isEqualTo(Question.CorrectAnswer.FAKE);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("updateCorrectAnswer() — throws ResourceNotFoundException when not found")
    void updateCorrectAnswer_notFound_throwsResourceNotFoundException() {
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.REAL);

        when(questionRepository.findById("P99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.updateCorrectAnswer("P99", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P99");

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCorrectAnswer() — can set REAL")
    void updateCorrectAnswer_setReal() {
        QuestionCorrectAnswerRequestDto request = new QuestionCorrectAnswerRequestDto();
        request.setCorrectAnswer(Question.CorrectAnswer.REAL);

        Question updated = Question.builder()
                .questionCode("1_exp1")
                .itemText("test")
                .questionType(Question.QuestionType.NEWS)
                .correctAnswer(Question.CorrectAnswer.REAL)
                .build();

        when(questionRepository.findById("1_exp1")).thenReturn(Optional.of(mockNewsQuestion));
        when(questionRepository.save(any())).thenReturn(updated);

        QuestionAdminResponseDto result = questionService.updateCorrectAnswer("1_exp1", request);

        assertThat(result.getCorrectAnswer()).isEqualTo(Question.CorrectAnswer.REAL);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Question> buildNewsList(int count, Question.NewsSet newsSet) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> Question.builder()
                        .questionCode(i + "_" + newsSet.name().toLowerCase())
                        .constructo("Test")
                        .itemText("News question " + i)
                        .questionType(Question.QuestionType.NEWS)
                        .newsSet(newsSet)
                        .correctAnswer(i % 2 == 0 ? Question.CorrectAnswer.REAL : Question.CorrectAnswer.FAKE)
                        .build())
                .toList();
    }
}
package com.konrad.konradquiz.service.interfaces;

import com.konrad.konradquiz.dto.request.QuestionCorrectAnswerRequestDto;
import com.konrad.konradquiz.dto.response.QuestionAdminResponseDto;
import com.konrad.konradquiz.entity.Question;
import java.util.List;

public interface IQuestionService {
    List<Question> getAllForSession();
    Question findEntityByCode(String questionCode);   // ← returns raw entity for internal service use
    long countAll();                                  // ← used by AnswerService for validation
    QuestionAdminResponseDto updateCorrectAnswer(String questionCode, QuestionCorrectAnswerRequestDto dto);
    List<Question> getAllProfileQuestions();
    List<Question> getNewsQuestionsForSet(Question.NewsSet newsSet);
    long countByType(Question.QuestionType questionType);
}
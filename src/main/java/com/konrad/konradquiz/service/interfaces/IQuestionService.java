package com.konrad.konradquiz.service.interfaces;

import com.konrad.konradquiz.dto.request.QuestionCorrectAnswerRequestDto;
import com.konrad.konradquiz.dto.response.QuestionAdminResponseDto;
import com.konrad.konradquiz.entity.Question;

import java.util.Collection;
import java.util.List;

public interface IQuestionService {

    /** Returns the entity or throws {@code ResourceNotFoundException}. For internal service use. */
    Question findEntityByCode(String questionCode);

    /** Bulk lookup for internal service use; codes that do not exist are simply absent from the result. */
    List<Question> findAllByCodes(Collection<String> questionCodes);

    QuestionAdminResponseDto updateCorrectAnswer(String questionCode, QuestionCorrectAnswerRequestDto dto);

    List<Question> getAllProfileQuestions();

    List<Question> getNewsQuestionsForSet(Question.NewsSet newsSet);

    long countByType(Question.QuestionType questionType);
}

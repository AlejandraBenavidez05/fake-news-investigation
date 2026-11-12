package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByQuestionType(Question.QuestionType questionType);

    List<Question> findByQuestionTypeAndNewsSet(Question.QuestionType questionType, Question.NewsSet newsSet);

    long countByQuestionType(Question.QuestionType questionType);
}

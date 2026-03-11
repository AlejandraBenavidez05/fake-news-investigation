package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {

    // For post-analysis queries
    List<Question> findByConstructo(String constructo);
    List<Question> findBySubCategory(String subCategory);
    List<Question> findByCorrectAnswerIsNull(); // find questions still missing FAKE/REAL
}
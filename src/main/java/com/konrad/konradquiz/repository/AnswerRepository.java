package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // Get all answers for one participant ordered by the random position they saw
    List<Answer> findByParticipantIdOrderByQuestionOrder(Long participantId);

    // Prevent duplicate answers
    boolean existsByParticipantIdAndQuestionQuestionCode(Long participantId, String questionCode);

    // Count how many questions a participant has answered (to detect completion)
    long countByParticipantId(Long participantId);

    // Post-analysis: get all answers for a specific question across all participants
    List<Answer> findByQuestionQuestionCode(String questionCode);

    // Post-analysis: average score per question
    @Query("SELECT AVG(a.score) FROM Answer a WHERE a.question.questionCode = :questionCode")
    Double findAverageScoreByQuestionCode(@Param("questionCode") String questionCode);
    // Fetches everything needed for export in a single DB query
    @Query("""
    SELECT a FROM Answer a
    JOIN FETCH a.participant p
    JOIN FETCH a.question q
    ORDER BY p.id, a.questionOrder
    """)
    List<Answer> findAllForExport();
}
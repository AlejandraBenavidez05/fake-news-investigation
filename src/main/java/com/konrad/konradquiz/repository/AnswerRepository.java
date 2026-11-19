package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** Used to reject a second batch submission from the same participant. */
    long countByParticipantId(Long participantId);

    /** Loads every answer with its participant and question in a single query for the CSV export. */
    @Query("""
            SELECT a FROM Answer a
            JOIN FETCH a.participant p
            JOIN FETCH a.question q
            ORDER BY p.id, a.questionOrder
            """)
    List<Answer> findAllForExport();
}

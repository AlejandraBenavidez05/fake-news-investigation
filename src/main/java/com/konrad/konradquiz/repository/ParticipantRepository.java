package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    /** Duplicate-registration check (emails are stored encrypted, so lookups go through the hash). */
    boolean existsByEmailHash(String emailHash);

    /** Number of participants per (feedbackTiming, presentationFormat) group, in a single query. */
    @Query("""
            SELECT new com.konrad.konradquiz.service.impl.GroupAssignmentService$GroupCount(
                p.feedbackTiming,
                p.presentationFormat,
                COUNT(p)
            )
            FROM Participant p
            GROUP BY p.feedbackTiming, p.presentationFormat
            """)
    List<GroupAssignmentService.GroupCount> countByGroup();

    long countByNewsSet(Question.NewsSet newsSet);
}

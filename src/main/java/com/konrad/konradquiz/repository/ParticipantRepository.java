package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEmailHash(String emailHash);        // ← duplicate check
    Optional<Participant> findByEmailHash(String emailHash);
    long countByFeedbackTimingAndPresentationFormat(
            Participant.FeedbackTiming feedbackTiming,
            Participant.PresentationFormat presentationFormat
    );

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
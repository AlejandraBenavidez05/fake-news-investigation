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

    // SQL Server / Azure SQL: serializes assignment requests across application instances.
    // Transaction ownership releases the lock automatically on commit or rollback.
    @Query(value = """
        DECLARE @lockResult int;
        EXEC @lockResult = sys.sp_getapplock
            @Resource = N'konradquiz:participant-assignment',
            @LockMode = 'Exclusive',
            @LockOwner = 'Transaction',
            @LockTimeout = 15000;
        SELECT @lockResult;
        """, nativeQuery = true)
    int lockGroupAssignment();

    @Query("""
        SELECT new com.konrad.konradquiz.service.impl.GroupAssignmentService$GroupCount(
            p.newsSet,
            p.feedbackTiming,
            p.presentationFormat,
            COUNT(p)
        )
        FROM Participant p
        WHERE p.completionTimeSeconds IS NOT NULL
        GROUP BY p.newsSet, p.feedbackTiming, p.presentationFormat
        """)
    List<GroupAssignmentService.GroupCount> countCompletedByAllGroups();

    long countByNewsSet(Question.NewsSet newsSet);
}

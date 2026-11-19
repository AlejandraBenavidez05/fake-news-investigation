package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Participant;
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    /** Duplicate-registration check (emails are stored encrypted, so lookups go through the hash). */
    boolean existsByEmailHash(String emailHash);

    /**
     * SQL Server / Azure SQL only: serializes group assignment across application instances.
     * The lock is owned by the current transaction and released on commit or rollback.
     * Returns the {@code sp_getapplock} result code (negative = lock not acquired).
     */
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

    /** Completed experiments (completion time recorded) per (newsSet, feedbackTiming, presentationFormat) cell. */
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
}

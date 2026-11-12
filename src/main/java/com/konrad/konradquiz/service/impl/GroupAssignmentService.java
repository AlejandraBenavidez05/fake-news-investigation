package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question.NewsSet;
import com.konrad.konradquiz.repository.ParticipantRepository;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupAssignmentService {

    private final ParticipantRepository participantRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long COMPLETED_TARGET_PER_FORMAT = 20L;

    private static final List<AssignmentPhase> ASSIGNMENT_PHASES = List.of(
            new AssignmentPhase(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B),
            new AssignmentPhase(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A),
            new AssignmentPhase(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B),
            new AssignmentPhase(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A)
    );

    private static final List<AssignedGroup> ALL_GROUPS = List.of(
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.TEXT),
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM),
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP),
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.TEXT),
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM),
            new AssignedGroup(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.TEXT),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.TEXT),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM),
            new AssignedGroup(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP)
    );

    public GroupAssignmentService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    /**
     * Counts completed experiments across all 12 experiment cells, using the
     * completionTimeSeconds marker written after a validated answer batch.
     * Each phase fills TEXT, INSTAGRAM and WHATSAPP to at least 20 completions:
     * ENVIRONMENT/GROUP_B, ENVIRONMENT/GROUP_A, TECHNOLOGY/GROUP_B, then
     * TECHNOLOGY/GROUP_A. Reaching 13 does not advance to the next phase.
     * Within the first unfinished phase, the least completed formats are chosen
     * with random ties. Once every phase reaches 20, all 12 cells are balanced.
     *
     * The caller must save the participant in the same transaction. The SQL Server
     * application lock serializes assignment requests until that transaction
     * commits or rolls back. An assignment does not reserve a completed slot:
     * active or abandoned sessions do not change counts until they complete.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssignedGroup assign() {
        int lockResult = participantRepository.lockGroupAssignment();
        if (lockResult < 0) {
            throw new CannotAcquireLockException(
                    "Could not acquire the participant assignment lock: " + lockResult
            );
        }

        Map<AssignedGroup, Long> counts = participantRepository.countCompletedByAllGroups().stream()
                .collect(Collectors.toMap(
                        c -> new AssignedGroup(c.newsSet(), c.feedbackTiming(), c.presentationFormat()),
                        GroupCount::count
                ));

        List<AssignedGroup> candidates = List.of();
        for (AssignmentPhase phase : ASSIGNMENT_PHASES) {
            candidates = ALL_GROUPS.stream()
                    .filter(group -> group.newsSet() == phase.newsSet()
                            && group.feedbackTiming() == phase.feedbackTiming())
                    .filter(group -> counts.getOrDefault(group, 0L) < COMPLETED_TARGET_PER_FORMAT)
                    .toList();
            if (!candidates.isEmpty()) {
                break;
            }
        }

        if (candidates.isEmpty()) {
            candidates = ALL_GROUPS;
        }

        long minimumCount = candidates.stream()
                .mapToLong(group -> counts.getOrDefault(group, 0L))
                .min()
                .orElseThrow();

        List<AssignedGroup> leastPopulated = candidates.stream()
                .filter(group -> counts.getOrDefault(group, 0L) == minimumCount)
                .toList();

        return leastPopulated.get(RANDOM.nextInt(leastPopulated.size()));
    }

    private record AssignmentPhase(NewsSet newsSet, FeedbackTiming feedbackTiming) {}

    public record AssignedGroup(
            NewsSet newsSet,
            FeedbackTiming feedbackTiming,
            PresentationFormat presentationFormat
    ) {}

    public record GroupCount(
            NewsSet newsSet,
            FeedbackTiming feedbackTiming,
            PresentationFormat presentationFormat,
            long count
    ) {}
}

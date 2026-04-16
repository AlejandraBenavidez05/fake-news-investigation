package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupAssignmentService {

    private final ParticipantRepository participantRepository;

    // All 6 combinations — defined once, reused across calls
    private static final List<AssignedGroup> ALL_GROUPS = List.of(
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM),
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP),
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.TEXT),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.TEXT)
    );

    /**
     * Assigns the group with the fewest current participants — single DB query.
     * Guarantees balanced distribution across all 6 groups.
     */
    @Transactional(readOnly = true)
    public AssignedGroup assignGroup() {
        // Single query returns counts for all 6 groups at once
        List<GroupCount> counts = participantRepository.countByGroup();

        return ALL_GROUPS.stream()
                .min(Comparator.comparingLong(group ->
                        counts.stream()
                                .filter(c -> c.feedbackTiming() == group.feedbackTiming()
                                        && c.presentationFormat() == group.presentationFormat())
                                .mapToLong(GroupCount::count)
                                .findFirst()
                                .orElse(0L)   // group has 0 participants yet
                ))
                .orElseThrow();   // can never be empty — ALL_GROUPS is hardcoded
    }

    public record AssignedGroup(
            FeedbackTiming feedbackTiming,
            PresentationFormat presentationFormat
    ) {}

    public record GroupCount(
            FeedbackTiming feedbackTiming,
            PresentationFormat presentationFormat,
            long count
    ) {}
}
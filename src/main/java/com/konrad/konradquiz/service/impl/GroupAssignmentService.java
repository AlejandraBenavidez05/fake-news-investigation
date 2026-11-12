package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupAssignmentService {

    /** All six experimental groups, in tie-breaking order: the first group with the fewest participants wins. */
    private static final List<AssignedGroup> ALL_GROUPS = List.of(
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM),
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP),
            new AssignedGroup(FeedbackTiming.GROUP_A, PresentationFormat.TEXT),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP),
            new AssignedGroup(FeedbackTiming.GROUP_B, PresentationFormat.TEXT)
    );

    private final ParticipantRepository participantRepository;

    /** Assigns the group with the fewest participants so far, keeping the six groups balanced. */
    @Transactional(readOnly = true)
    public AssignedGroup assignGroup() {
        Map<AssignedGroup, Long> participantsByGroup = participantRepository.countByGroup().stream()
                .collect(Collectors.toMap(
                        count -> new AssignedGroup(count.feedbackTiming(), count.presentationFormat()),
                        GroupCount::count));

        return ALL_GROUPS.stream()
                .min(Comparator.comparingLong(group -> participantsByGroup.getOrDefault(group, 0L)))
                .orElseThrow();   // never empty: ALL_GROUPS is a non-empty constant
    }

    /** Assigns the news set (ENVIRONMENT or TECHNOLOGY) with fewer participants; ties go to ENVIRONMENT. */
    @Transactional(readOnly = true)
    public Question.NewsSet assignNewsSet() {
        long environmentCount = participantRepository.countByNewsSet(Question.NewsSet.ENVIRONMENT);
        long technologyCount = participantRepository.countByNewsSet(Question.NewsSet.TECHNOLOGY);

        return environmentCount <= technologyCount
                ? Question.NewsSet.ENVIRONMENT
                : Question.NewsSet.TECHNOLOGY;
    }

    public record AssignedGroup(FeedbackTiming feedbackTiming, PresentationFormat presentationFormat) {
    }

    /** JPQL constructor-expression target of {@link ParticipantRepository#countByGroup()}. */
    public record GroupCount(FeedbackTiming feedbackTiming, PresentationFormat presentationFormat, long count) {
    }
}

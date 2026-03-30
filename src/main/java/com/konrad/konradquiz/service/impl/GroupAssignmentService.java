package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GroupAssignmentService {

    private final ParticipantRepository participantRepository;

    // All 6 possible combinations
    private static final FeedbackTiming[] TIMINGS = {
            FeedbackTiming.GROUP_A, FeedbackTiming.GROUP_A, FeedbackTiming.GROUP_A,
            FeedbackTiming.GROUP_B, FeedbackTiming.GROUP_B, FeedbackTiming.GROUP_B
    };

    private static final PresentationFormat[] FORMATS = {
            PresentationFormat.INSTAGRAM,
            PresentationFormat.WHATSAPP,
            PresentationFormat.TEXT,
            PresentationFormat.INSTAGRAM,
            PresentationFormat.WHATSAPP,
            PresentationFormat.TEXT
    };

    /**
     * Assigns the group with the fewest current participants.
     * Guarantees balanced distribution across all 6 groups.
     */
    public AssignedGroup assignGroup() {
        Objects.requireNonNull(participantRepository, "ParticipantRepository must not be null");

        int minCount = Integer.MAX_VALUE;
        int assignedIndex = 0;

        for (int i = 0; i < TIMINGS.length; i++) {
            FeedbackTiming timing = Objects.requireNonNull(TIMINGS[i], "FeedbackTiming must not be null");
            PresentationFormat format = Objects.requireNonNull(FORMATS[i], "PresentationFormat must not be null");

            long count = participantRepository.countByFeedbackTimingAndPresentationFormat(timing, format);

            if (count < minCount) {
                minCount = (int) count;
                assignedIndex = i;
            }
        }

        return new AssignedGroup(TIMINGS[assignedIndex], FORMATS[assignedIndex]);
    }

    public record AssignedGroup(FeedbackTiming feedbackTiming, PresentationFormat presentationFormat) {}
}

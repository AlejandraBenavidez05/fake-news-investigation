package com.konrad.konradquiz.service;

import com.konrad.konradquiz.entity.Participant.FeedbackTiming;
import com.konrad.konradquiz.entity.Participant.PresentationFormat;
import com.konrad.konradquiz.entity.Question.NewsSet;
import com.konrad.konradquiz.repository.ParticipantRepository;
import com.konrad.konradquiz.service.impl.GroupAssignmentService;
import com.konrad.konradquiz.service.impl.GroupAssignmentService.AssignedGroup;
import com.konrad.konradquiz.service.impl.GroupAssignmentService.GroupCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAssignmentServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private GroupAssignmentService groupAssignmentService;

    @Test
    void prioritizesTheTwoEnvironmentGroupBFormatsAt12InTheLatestSnapshot() {
        useCounts(reportedCounts());

        assertThat(groupAssignmentService.assign()).isIn(
                group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM),
                group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP));
    }

    @Test
    void reaching13KeepsEnvironmentGroupBPrioritizedUntil20() {
        Map<AssignedGroup, Long> counts = reportedCounts();
        setCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 13L);
        useCounts(counts);

        AssignedGroup assignment = groupAssignmentService.assign();

        assertCohort(assignment, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B);
        assertThat(counts.get(assignment)).isEqualTo(13L);
    }

    @Test
    void completesEnvironmentGroupBAfter23CompletionsThenMovesToEnvironmentGroupA() {
        Map<AssignedGroup, Long> counts = reportedCounts();
        Map<AssignedGroup, Long> before = Map.copyOf(counts);
        useCounts(counts);

        completeCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 23);

        assertCohortCount(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 20L);
        before.forEach((cell, count) -> {
            if (cell.newsSet() != NewsSet.ENVIRONMENT || cell.feedbackTiming() != FeedbackTiming.GROUP_B) {
                assertThat(counts.get(cell)).isEqualTo(count);
            }
        });
        assertThat(groupAssignmentService.assign()).isEqualTo(
                group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP));
    }

    @Test
    void everyPhaseReaches20BeforeTheNextPhaseStarts() {
        Map<AssignedGroup, Long> counts = reportedCounts();
        useCounts(counts);

        completeCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 23);
        assertCohortCount(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 20L);
        completeCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, 38);
        assertCohortCount(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, 20L);
        completeCohort(counts, NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, 38);
        assertCohortCount(counts, NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, 20L);
        completeCohort(counts, NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, 43);

        assertThat(counts.values()).containsOnly(20L);
    }

    @Test
    void anOverfilledFormatDoesNotSkipAnUnfinishedFormatOrRaiseTheTarget() {
        Map<AssignedGroup, Long> counts = reportedCounts();
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.TEXT), 25L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM), 20L);
        AssignedGroup unfinished = group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B,
                PresentationFormat.WHATSAPP);
        counts.put(unfinished, 19L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.TEXT), 100L);
        useCounts(counts);

        assertThat(groupAssignmentService.assign()).isEqualTo(unfinished);
        increment(counts, unfinished);
        assertCohort(groupAssignmentService.assign(), NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A);
    }

    @Test
    void technologyWaitsUntilEveryEnvironmentGroupAFormatHas20Completions() {
        Map<AssignedGroup, Long> counts = reportedCounts();
        setCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 20L);
        setCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, 20L);
        AssignedGroup unfinished = group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A,
                PresentationFormat.INSTAGRAM);
        counts.put(unfinished, 19L);
        useCounts(counts);

        assertThat(groupAssignmentService.assign()).isEqualTo(unfinished);
        increment(counts, unfinished);
        assertThat(groupAssignmentService.assign()).isEqualTo(
                group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP));
    }

    @Test
    void resumesGlobalBalancingAfterAll12CellsReach20() {
        Map<AssignedGroup, Long> counts = uniformCounts(20L);
        useCounts(counts);

        for (int i = 0; i < 120; i++) {
            AssignedGroup assignment = groupAssignmentService.assign();
            long minimum = counts.values().stream().mapToLong(Long::longValue).min().orElseThrow();
            assertThat(counts.get(assignment)).isEqualTo(minimum);
            increment(counts, assignment);
            long maximum = counts.values().stream().mapToLong(Long::longValue).max().orElseThrow();
            long newMinimum = counts.values().stream().mapToLong(Long::longValue).min().orElseThrow();
            assertThat(maximum - newMinimum).isLessThanOrEqualTo(1L);
        }
        assertThat(counts.values()).containsOnly(30L);
    }

    @Test
    void unfinishedRegistrationsDoNotAdvanceThePhase() {
        Map<AssignedGroup, Long> completedCounts = reportedCounts();
        setCohort(completedCounts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 20L);
        AssignedGroup unfinished = group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B,
                PresentationFormat.TEXT);
        completedCounts.put(unfinished, 19L);
        Map<AssignedGroup, Long> before = Map.copyOf(completedCounts);
        useCounts(completedCounts);

        for (int i = 0; i < 30; i++) {
            assertThat(groupAssignmentService.assign()).isEqualTo(unfinished);
        }

        assertThat(completedCounts).isEqualTo(before);
        increment(completedCounts, unfinished);
        assertCohort(groupAssignmentService.assign(), NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A);
    }

    @Test
    void missingRowsCountAsZeroAndLegacyNullNewsSetsDoNotCompleteAValidPhase() {
        Map<AssignedGroup, Long> counts = uniformCounts(0L);
        List<GroupCount> extraRows = List.of(
                new GroupCount(null, FeedbackTiming.GROUP_B, PresentationFormat.TEXT, 1_000_000L));
        useCounts(counts, extraRows);

        completeCohort(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 60);

        assertCohortCount(counts, NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, 20L);
        assertCohort(groupAssignmentService.assign(), NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A);
    }

    @Test
    void acquiresTheTransactionLockBeforeReadingCompletedCounts() {
        useCounts(uniformCounts(0L));

        groupAssignmentService.assign();

        var calls = inOrder(participantRepository);
        calls.verify(participantRepository).lockGroupAssignment();
        calls.verify(participantRepository).countCompletedByAllGroups();
    }

    @Test
    void abortsWithoutReadingCountsWhenTheLockCannotBeAcquired() {
        when(participantRepository.lockGroupAssignment()).thenReturn(-1);

        assertThatThrownBy(() -> groupAssignmentService.assign())
                .isInstanceOf(CannotAcquireLockException.class);

        verify(participantRepository, never()).countCompletedByAllGroups();
    }

    private void completeCohort(Map<AssignedGroup, Long> counts, NewsSet set,
                                FeedbackTiming timing, int completions) {
        for (int i = 0; i < completions; i++) {
            AssignedGroup assignment = groupAssignmentService.assign();
            assertCohort(assignment, set, timing);
            assertThat(counts.get(assignment)).isLessThan(20L);
            long cohortMinimum = counts.entrySet().stream()
                    .filter(entry -> entry.getKey().newsSet() == set
                            && entry.getKey().feedbackTiming() == timing)
                    .mapToLong(Map.Entry::getValue).min().orElseThrow();
            assertThat(counts.get(assignment)).isEqualTo(cohortMinimum);
            increment(counts, assignment);
        }
    }

    private void useCounts(Map<AssignedGroup, Long> counts) {
        useCounts(counts, List.of());
    }

    private void useCounts(Map<AssignedGroup, Long> counts, List<GroupCount> extraRows) {
        when(participantRepository.countCompletedByAllGroups()).thenAnswer(ignored -> {
            List<GroupCount> rows = new ArrayList<>(extraRows);
            counts.forEach((cell, count) -> {
                if (count > 0L) {
                    rows.add(new GroupCount(cell.newsSet(), cell.feedbackTiming(),
                            cell.presentationFormat(), count));
                }
            });
            return rows;
        });
    }

    private static Map<AssignedGroup, Long> reportedCounts() {
        Map<AssignedGroup, Long> counts = uniformCounts(0L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.TEXT), 13L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM), 8L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP), 1L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.TEXT), 13L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM), 12L);
        counts.put(group(NewsSet.ENVIRONMENT, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP), 12L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.TEXT), 3L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.INSTAGRAM), 1L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_A, PresentationFormat.WHATSAPP), 13L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.TEXT), 8L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.INSTAGRAM), 12L);
        counts.put(group(NewsSet.TECHNOLOGY, FeedbackTiming.GROUP_B, PresentationFormat.WHATSAPP), 2L);
        return counts;
    }

    private static Map<AssignedGroup, Long> uniformCounts(long count) {
        Map<AssignedGroup, Long> counts = new LinkedHashMap<>();
        for (NewsSet set : NewsSet.values()) {
            for (FeedbackTiming timing : FeedbackTiming.values()) {
                for (PresentationFormat format : PresentationFormat.values()) {
                    counts.put(group(set, timing, format), count);
                }
            }
        }
        return counts;
    }

    private static void setCohort(Map<AssignedGroup, Long> counts, NewsSet set,
                                  FeedbackTiming timing, long count) {
        for (PresentationFormat format : PresentationFormat.values()) {
            counts.put(group(set, timing, format), count);
        }
    }

    private static void assertCohortCount(Map<AssignedGroup, Long> counts, NewsSet set,
                                          FeedbackTiming timing, long count) {
        for (PresentationFormat format : PresentationFormat.values()) {
            assertThat(counts.get(group(set, timing, format))).isEqualTo(count);
        }
    }

    private static void assertCohort(AssignedGroup assignment, NewsSet set, FeedbackTiming timing) {
        assertThat(assignment.newsSet()).isEqualTo(set);
        assertThat(assignment.feedbackTiming()).isEqualTo(timing);
    }

    private static AssignedGroup group(NewsSet set, FeedbackTiming timing, PresentationFormat format) {
        return new AssignedGroup(set, timing, format);
    }

    private static void increment(Map<AssignedGroup, Long> counts, AssignedGroup assignment) {
        counts.compute(assignment, (ignored, count) -> count + 1L);
    }
}

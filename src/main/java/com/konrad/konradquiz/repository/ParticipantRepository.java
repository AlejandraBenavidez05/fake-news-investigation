package com.konrad.konradquiz.repository;

import com.konrad.konradquiz.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEmailHash(String emailHash);        // ← duplicate check
    Optional<Participant> findByEmailHash(String emailHash);
    long countByFeedbackTimingAndPresentationFormat(
            Participant.FeedbackTiming feedbackTiming,
            Participant.PresentationFormat presentationFormat
    );
}
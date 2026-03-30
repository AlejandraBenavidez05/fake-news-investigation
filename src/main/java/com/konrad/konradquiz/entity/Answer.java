package com.konrad.konradquiz.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "answers",
        uniqueConstraints = {
                // One answer per question per participant — no duplicates ever
                @UniqueConstraint(
                        name = "uq_answer_participant_question",
                        columnNames = {"participant_id", "question_code"}
                )
        },
        indexes = {
                @Index(name = "idx_answer_participant", columnList = "participant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Score given by participant:
     * 0   = completely sure it's FAKE
     * 100 = completely sure it's REAL
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * The random position (1, 2, 3...) this question was shown
     * to THIS participant. Stored so we can analyze if question
     * order influenced responses.
     */
    @Column(nullable = false)
    private Integer questionOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_code", nullable = false)
    private Question question;

    public enum SdtCategory {
        HIT,              // (A) News is FAKE + participant said FAKE ✅
        MISS,             // (C) News is FAKE + participant said REAL ❌
        FALSE_ALARM,      // (B) News is REAL + participant said FAKE ❌
        CORRECT_RESPONSE, // (D) News is REAL + participant said REAL ✅
        UNSURE            // score exactly 50 — participant couldn't decide
    }
}
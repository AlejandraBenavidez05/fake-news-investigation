package com.konrad.konradquiz.entity;

import com.konrad.konradquiz.converter.StringEncryptionConverter;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participants",
        indexes = {
                // Index on emailHash — NOT on email (email is encrypted, can't be indexed)
                @Index(name = "idx_participant_email_hash", columnList = "email_hash", unique = true)
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String alias;

    @Convert(converter = StringEncryptionConverter.class)  // ← Jasypt encrypts/decrypts
    @Column(nullable = false, length = 500)                // ← 500 because encrypted value is longer
    private String email;

    @Column(name = "email_hash", nullable = false, length = 64, unique = true)
    private String emailHash;                              // ← SHA-256 hash for duplicate check

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackTiming feedbackTiming;      // GROUP_A or GROUP_B

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresentationFormat presentationFormat;  // INSTAGRAM, WHATSAPP, TEXT

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String region;                  // e.g. "Bogotá", "Cali", "Madrid"

    @Column
    private Long completionTimeSeconds;     // null until answers submitted, then set

    public enum Sex { MALE, FEMALE, OTHER }

    public enum FeedbackTiming {
        GROUP_A,  // feedback after EACH question
        GROUP_B   // feedback only AFTER ALL questions
    }

    public enum PresentationFormat {
        INSTAGRAM,
        WHATSAPP,
        TEXT
    }
}
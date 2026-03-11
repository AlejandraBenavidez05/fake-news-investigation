package com.konrad.konradquiz.entity;

import com.konrad.konradquiz.converter.StringEncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
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
    @Column(nullable = false, length = 10)
    private ExperimentGroup experimentGroup;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    public enum Sex { MALE, FEMALE, OTHER }

    public enum ExperimentGroup {
        GROUP_A,  // sees correct answer after EACH question
        GROUP_B   // sees correct answers only AFTER finishing ALL questions
    }
}
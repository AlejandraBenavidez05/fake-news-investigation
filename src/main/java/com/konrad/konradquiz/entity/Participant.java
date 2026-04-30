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
                @Index(name = "idx_participant_email_hash", columnList = "email_hash", unique = true),
                @Index(name = "idx_participant_group", columnList = "feedback_timing, presentation_format")
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

    @Column(name = "alias", nullable = false, length = 100)
    private String alias;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(name = "email", nullable = false, length = 500)
    private String email;

    @Column(name = "email_hash", nullable = false, length = 64, unique = true)
    private String emailHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_timing", nullable = false, length = 20)
    private FeedbackTiming feedbackTiming;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_format", nullable = false, length = 20)
    private PresentationFormat presentationFormat;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "completion_time_seconds")
    private Long completionTimeSeconds;

    @Column(name = "consent_academic_purpose", nullable = false)
    private Boolean consentAcademicPurpose;

    @Column(name = "consent_participation_process", nullable = false)
    private Boolean consentParticipationProcess;

    @Column(name = "consent_data_processing", nullable = false)
    private Boolean consentDataProcessing;

    @Column(name = "consent_no_risk", nullable = false)
    private Boolean consentNoRisk;

    @Column(name = "consent_no_payment", nullable = false)
    private Boolean consentNoPayment;

    @Column(name = "consent_project_info", nullable = false)
    private Boolean consentProjectInfo;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Question.NewsSet newsSet;            // ENVIRONMENT or TECHNOLOGY

    public enum Sex { MALE, FEMALE, OTHER }

    public enum FeedbackTiming { GROUP_A, GROUP_B }

    public enum PresentationFormat { INSTAGRAM, WHATSAPP, TEXT }
}
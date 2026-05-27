package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commitments")
public class Commitment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_message_id")
    private RawMessage sourceMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommitmentCategory category = CommitmentCategory.COMMITMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommitmentDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommitmentStatus status = CommitmentStatus.PENDING;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "due_date_confidence", length = 20)
    private DueDateConfidence dueDateConfidence;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_source", nullable = false, length = 20)
    private ExtractionSource extractionSource = ExtractionSource.LLM;

    @Column(name = "raw_trigger_text", columnDefinition = "TEXT")
    private String rawTriggerText;

    @Column(name = "urgency_score", precision = 3, scale = 2)
    private BigDecimal urgencyScore;

    @Column(name = "priority_score", precision = 3, scale = 2)
    private BigDecimal priorityScore;

    @Column(name = "fulfilled_at")
    private OffsetDateTime fulfilledAt;

    @Column(name = "snoozed_until")
    private OffsetDateTime snoozedUntil;

    @OneToMany(mappedBy = "commitment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CommitmentParticipant> participants = new ArrayList<>();

    public void fulfill() {
        this.status = CommitmentStatus.FULFILLED;
        this.fulfilledAt = OffsetDateTime.now();
    }

    public void snooze(OffsetDateTime until) {
        this.status = CommitmentStatus.SNOOZED;
        this.snoozedUntil = until;
    }

    public void markOverdue() {
        this.status = CommitmentStatus.OVERDUE;
    }
}

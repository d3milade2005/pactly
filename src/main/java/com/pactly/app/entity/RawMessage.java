package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "raw_messages")
public class RawMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageSource source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    // SHA-256(source + external_id + user_id) -> idempotency key
    @Column(name = "message_fingerprint", nullable = false, unique = true, length = 64)
    private String messageFingerprint;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "recipient_emails", columnDefinition = "text[]")
    private String[] recipientEmails;

    @Column(name = "body_snippet", columnDefinition = "TEXT")
    private String bodySnippet;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public void markProcessed() {
        this.processingStatus = ProcessingStatus.PROCESSED;
        this.processedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = OffsetDateTime.now();
    }
}

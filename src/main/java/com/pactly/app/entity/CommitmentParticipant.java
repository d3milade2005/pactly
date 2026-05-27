package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "commitment_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"commitment_id", "contact_id", "role"})
)
public class CommitmentParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commitment_id", nullable = false)
    private Commitment commitment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public CommitmentParticipant(Commitment commitment, Contact contact, ParticipantRole role) {
        this.commitment = commitment;
        this.contact = contact;
        this.role = role;
    }
}

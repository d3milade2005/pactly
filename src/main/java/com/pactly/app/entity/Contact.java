package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contacts", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "email"}))
public class Contact {
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

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "relationship_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal relationshipScore = new BigDecimal("0.50");

    public Contact(User user, String email, String displayName) {
        this.user = user;
        this.email = email;
        this.displayName = displayName;
    }
}

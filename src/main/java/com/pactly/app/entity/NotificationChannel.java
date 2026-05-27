package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_channels")
public class NotificationChannel {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    // Email address, phone number, or webhook URL
    @Column(nullable = false, columnDefinition = "TEXT")
    private String destination;

    // User-friendly label: "My WhatsApp", "Work Email"
    @Column(length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public NotificationChannel(User user, ChannelType channelType, String destination, String label) {
        this.user = user;
        this.channelType = channelType;
        this.destination = destination;
        this.label = label;
    }
}

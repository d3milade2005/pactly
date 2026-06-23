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
@Entity
@NoArgsConstructor
@Table(name = "gmail_sync_state")
public class GmailSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "last_history_id")
    private String lastHistoryId;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public GmailSyncState(User user) {
        this.user = user;
    }

    public boolean isFirstSync() {
        return lastHistoryId == null;
    }

    public void updateAfterSync(String historyId) {
        this.lastHistoryId = historyId;
        this.lastSyncedAt = OffsetDateTime.now();
    }
}

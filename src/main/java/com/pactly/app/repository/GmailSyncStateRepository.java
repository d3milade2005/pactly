package com.pactly.app.repository;

import com.pactly.app.entity.GmailSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GmailSyncStateRepository extends JpaRepository<GmailSyncState, UUID> {
    Optional<GmailSyncState> findByUserId(UUID userId);
}

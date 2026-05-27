package com.pactly.app.repository;

import com.pactly.app.entity.ProcessingStatus;
import com.pactly.app.entity.RawMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawMessageRepository extends JpaRepository<RawMessage, UUID> {
    boolean existsByMessageFingerprint(String messageFingerprint);
    Optional<RawMessage> findByMessageFingerprint(String messageFingerprint);
    List<RawMessage> findByUserIdAndProcessingStatus(UUID userId, ProcessingStatus status);
}

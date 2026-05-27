package com.pactly.app.repository;

import com.pactly.app.entity.OutboxEvent;
import com.pactly.app.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // Poller picks these up in order
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    // Retry -> don't retry events that have failed too many times
    List<OutboxEvent> findByStatusAndRetryCountLessThan(OutboxEventStatus status, int maxRetries);
}

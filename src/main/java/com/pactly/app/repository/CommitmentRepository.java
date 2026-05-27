package com.pactly.app.repository;

import com.pactly.app.entity.Commitment;
import com.pactly.app.entity.CommitmentDirection;
import com.pactly.app.entity.CommitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommitmentRepository extends JpaRepository<Commitment, UUID> {
    List<Commitment> findByUserIdAndDirection(UUID userId, CommitmentDirection direction);
    List<Commitment> findByUserIdAndStatus(UUID userId, CommitmentStatus status);
    Page<Commitment> findByUserId(UUID userId, Pageable pageable);

    // Nudge scheduler -> find pending commitments past their due date
    @Query("SELECT c FROM Commitment c WHERE c.status = 'PENDING' AND c.dueDate < :now")
    List<Commitment> findOverdue(OffsetDateTime now);

    // Nudge scheduler -> find commitments due within a window
    @Query("SELECT c FROM Commitment c WHERE c.status = 'PENDING' AND c.dueDate BETWEEN :from AND :to")
    List<Commitment> findDueBetween(OffsetDateTime from, OffsetDateTime to);
}

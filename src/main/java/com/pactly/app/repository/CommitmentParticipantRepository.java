package com.pactly.app.repository;

import com.pactly.app.entity.CommitmentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommitmentParticipantRepository extends JpaRepository<CommitmentParticipant, UUID> {
    List<CommitmentParticipant> findByCommitmentId(UUID commitmentId);
    List<CommitmentParticipant> findByContactId(UUID contactId);
}

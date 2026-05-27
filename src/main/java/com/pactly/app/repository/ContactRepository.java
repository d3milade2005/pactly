package com.pactly.app.repository;

import com.pactly.app.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Optional<Contact> findByUserIdAndEmail(UUID userId, String email);
    List<Contact> findByUserId(UUID userId);
}

package com.pactly.app.repository;

import com.pactly.app.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {
    List<NotificationChannel> findByUserIdAndActiveTrue(UUID userId);
    List<NotificationChannel> findByUserId(UUID userId);
}

package com.pactly.app.repository;

import com.pactly.app.entity.OauthProvider;
import com.pactly.app.entity.OauthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OauthTokenRepository extends JpaRepository<OauthToken, UUID> {
    Optional<OauthToken> findByUserIdAndProvider(UUID userId, OauthProvider provider);
    boolean existsByUserIdAndProvider(UUID userId, OauthProvider provider);
}

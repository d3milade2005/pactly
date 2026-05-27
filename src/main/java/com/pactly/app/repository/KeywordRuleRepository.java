package com.pactly.app.repository;

import com.pactly.app.entity.KeywordRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeywordRuleRepository extends JpaRepository<KeywordRule, UUID> {
    // Loaded for every email processed -> only fetch active rules
    @Query("SELECT k FROM KeywordRule k LEFT JOIN FETCH k.channels WHERE k.user.id = :userId AND k.active = true")
    List<KeywordRule> findActiveRulesWithChannels(UUID userId);
    List<KeywordRule> findByUserId(UUID userId);
}

package com.pactly.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "keyword_rules")
public class KeywordRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Human-readable rule name: "Job opportunities"
    @Column(nullable = false, length = 100)
    private String name;

    // ["interview", "offer letter", "salary negotiation"]
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] keywords;

    // false = alert if ANY keyword matches
    // true  = alert only if ALL keywords match
    @Column(name = "match_all", nullable = false)
    private boolean matchAll = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "keyword_rule_channels",
            joinColumns = @JoinColumn(name = "keyword_rule_id"),
            inverseJoinColumns = @JoinColumn(name = "notification_channel_id")
    )
    private List<NotificationChannel> channels = new ArrayList<>();

    public KeywordRule(User user, String name, String[] keywords, boolean matchAll) {
        this.user = user;
        this.name = name;
        this.keywords = keywords;
        this.matchAll = matchAll;
    }

    // Called during email processing to check if this rule fires
    public boolean matches(String text) {
        if (text == null || keywords == null) return false;
        String lowerText = text.toLowerCase();

        if (matchAll) {
            for (String keyword : keywords) {
                if (!lowerText.contains(keyword.toLowerCase())) return false;
            }
            return true;
        } else {
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) return true;
            }
            return false;
        }
    }
}

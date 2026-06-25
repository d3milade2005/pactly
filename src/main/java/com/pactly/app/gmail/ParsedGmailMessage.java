package com.pactly.app.gmail;

import java.time.OffsetDateTime;

public record ParsedGmailMessage(
        String externalId,
        String subject,
        String senderEmail,
        String[] recipientEmails,
        String bodySnippet,
        String fullBody,
        OffsetDateTime receivedAt
) {}

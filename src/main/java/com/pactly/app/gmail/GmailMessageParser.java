package com.pactly.app.gmail;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class GmailMessageParser {

    public ParsedGmailMessage parse(Message message) {
        String subject = getHeader(message, "Subject");
        String from    = getHeader(message, "From");
        String to      = getHeader(message, "To");
        String cc      = getHeader(message, "Cc");

        String senderEmail = extractEmail(from);

        List<String> recipients = new ArrayList<>();
        if (to != null) {
            Arrays.stream(to.split(","))
                    .map(this::extractEmail)
                    .forEach(recipients::add);
        }
        if (cc != null) {
            Arrays.stream(cc.split(","))
                    .map(this::extractEmail)
                    .forEach(recipients::add);
        }

        OffsetDateTime receivedAt = message.getInternalDate() != null
                ? Instant.ofEpochMilli(message.getInternalDate())
                  .atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        String fullBody = extractBody(message.getPayload());
        String snippet  = message.getSnippet();

        return new ParsedGmailMessage(
                message.getId(),
                subject,
                senderEmail,
                recipients.toArray(String[]::new),
                snippet,
                fullBody != null ? fullBody : snippet,
                receivedAt
        );
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    // Handles both "email@example.com" and "Name <email@example.com>"
    private String extractEmail(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        int start = raw.indexOf('<');
        int end   = raw.indexOf('>');
        if (start >= 0 && end > start) {
            return raw.substring(start + 1, end).trim();
        }
        return raw;
    }

    private String extractBody(MessagePart payload) {
        if (payload == null) return null;

        // Single-part plain text
        if ("text/plain".equals(payload.getMimeType())
                && payload.getBody() != null) {
            return decodeBase64(payload.getBody().getData());
        }

        if (payload.getParts() == null) return null;

        // Prefer plain text in multipart
        for (MessagePart part : payload.getParts()) {
            if ("text/plain".equals(part.getMimeType())
                    && part.getBody() != null) {
                return decodeBase64(part.getBody().getData());
            }
        }

        // Fall back to HTML if no plain text
        for (MessagePart part : payload.getParts()) {
            if ("text/html".equals(part.getMimeType())
                    && part.getBody() != null) {
                return decodeBase64(part.getBody().getData());
            }
        }

        return null;
    }

    private String decodeBase64(String data) {
        if (data == null) return null;
        try {
            return new String(Base64.getUrlDecoder().decode(data));
        } catch (Exception e) {
            log.warn("Failed to decode base64 email body", e);
            return null;
        }
    }
}
package com.pactly.app.gmail;

import com.google.api.services.gmail.model.Message;
import java.util.List;

public record GmailFetchResult(
        List<Message> messages,
        String latestHistoryId   // null if no messages found
) {}
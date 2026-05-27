package com.pactly.app.entity;

public enum OutboxEventStatus {
    PENDING,
    DISPATCHED,
    FAILED
}

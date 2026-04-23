package com.smartfarm.shared.domain;

/**
 * 领域事件基类
 */
public abstract class DomainEvent {
    private final long occurredAt = System.currentTimeMillis();

    public long getOccurredAt() {
        return occurredAt;
    }
}

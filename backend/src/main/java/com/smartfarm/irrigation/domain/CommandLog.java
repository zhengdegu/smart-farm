package com.smartfarm.irrigation.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "command_log")
@Getter @Setter
@NoArgsConstructor
public class CommandLog extends BaseEntity {

    @Column(name = "cmd_id", unique = true, nullable = false, length = 64)
    private String cmdId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private CommandAction action;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, Object> params;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private CommandStatus status = CommandStatus.CREATED;

    @Column
    private Integer priority = 0;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retry")
    private Integer maxRetry = 3;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, Object> result;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_code", length = 10)
    private String errorCode;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(length = 128)
    private String sign;

    public enum CommandAction { OPEN_VALVE, CLOSE_VALVE, SET_CONFIG }
    public enum CommandStatus { CREATED, SENT, CONFIRMED, EXECUTED, FAILED }

    public void markSent() {
        this.status = CommandStatus.SENT;
        this.sentAt = OffsetDateTime.now();
    }

    public void markConfirmed() {
        this.status = CommandStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now();
    }

    public void markExecuted(java.util.Map<String, Object> result) {
        this.status = CommandStatus.EXECUTED;
        this.executedAt = OffsetDateTime.now();
        this.result = result;
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = CommandStatus.FAILED;
        this.failedAt = OffsetDateTime.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetry;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}

package com.smartfarm.alert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alert")
@Getter @Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(nullable = false, length = 4)
    @Enumerated(EnumType.STRING)
    private AlertLevel level;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.PENDING;

    @Column
    private Boolean notified = false;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum AlertLevel { L1, L2, L3 }
    public enum AlertStatus { PENDING, ACKNOWLEDGED, RESOLVED }

    public void acknowledge(Long userId) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedBy = userId;
        this.acknowledgedAt = OffsetDateTime.now();
    }

    public void resolve() {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = OffsetDateTime.now();
    }
}

package com.smartfarm.irrigation.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "irrigation_rule")
@Getter @Setter
@NoArgsConstructor
public class IrrigationRule extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "rule_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "sensor_device_id", length = 64)
    private String sensorDeviceId;

    @Column(name = "threshold_low")
    private Double thresholdLow;

    @Column(name = "threshold_high")
    private Double thresholdHigh;

    @Column(name = "cron_expression", length = 64)
    private String cronExpression;

    @Column(name = "duration_min")
    private Integer durationMin = 30;

    @Column
    private Boolean enabled = true;

    @Column(name = "last_triggered_at")
    private OffsetDateTime lastTriggeredAt;

    public enum RuleType { THRESHOLD, SCHEDULE }
}

package com.smartfarm.telemetry.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sensor_data")
@Getter @Setter
@NoArgsConstructor
@IdClass(SensorDataId.class)
public class SensorData {

    @Id
    @Column(name = "time", nullable = false)
    private OffsetDateTime time;

    @Id
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(nullable = false)
    private Double value;

    @Column(length = 16)
    private String unit;

    @Column
    private Integer quality = 100;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, Object> rawPayload;
}

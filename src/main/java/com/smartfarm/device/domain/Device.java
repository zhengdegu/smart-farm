package com.smartfarm.device.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device")
@Getter @Setter
@NoArgsConstructor
public class Device extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "device_id", unique = true, nullable = false, length = 64)
    private String deviceId;

    @Column(name = "device_secret_encrypted", nullable = false)
    private byte[] deviceSecretEncrypted;

    @Column(name = "device_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(length = 100)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(name = "greenhouse_no", length = 20)
    private String greenhouseNo;

    @Column(length = 16)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "last_online_at")
    private OffsetDateTime lastOnlineAt;

    @Column(name = "firmware_version", length = 32)
    private String firmwareVersion;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, Object> metadata;

    public enum DeviceType { SENSOR, VALVE, GATEWAY }
    public enum DeviceStatus { ONLINE, OFFLINE, FAULT }

    public void goOnline() {
        this.status = DeviceStatus.ONLINE;
        this.lastOnlineAt = OffsetDateTime.now();
    }

    public void goOffline() {
        this.status = DeviceStatus.OFFLINE;
    }
}

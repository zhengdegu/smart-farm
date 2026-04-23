package com.smartfarm.fertigation.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 水肥执行记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fertigation_log")
public class FertigationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    private String deviceId;          // 施肥泵设备ID
    private String valveDeviceId;     // 关联灌溉阀门
    private String greenhouseNo;

    private Long recipeId;            // 使用的配方

    @Enumerated(EnumType.STRING)
    private FertigationStatus status = FertigationStatus.CREATED;

    private Double concentrationMlPerL;
    private Integer durationMin;
    private Double estimatedFertilizerMl; // 估算施肥量

    private String triggeredBy;       // AUTO:rule_x / USER:xxx
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String failureReason;

    public enum FertigationStatus { CREATED, RUNNING, COMPLETED, FAILED }
}

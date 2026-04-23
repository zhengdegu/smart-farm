package com.smartfarm.tenant.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tenant")
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;          // 租户编码 e.g. "t001"

    @Column(nullable = false)
    private String name;          // 租户名称 e.g. "寿光示范基地"

    private String contactName;
    private String contactPhone;
    private String address;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlan plan = TenantPlan.BASIC;

    private Integer maxDevices = 20;       // 最大设备数
    private Integer maxUsers = 5;          // 最大用户数
    private Integer dataRetentionDays = 90; // 数据保留天数

    public enum TenantStatus { ACTIVE, SUSPENDED, EXPIRED }
    public enum TenantPlan { TRIAL, BASIC, STANDARD, ENTERPRISE }
}

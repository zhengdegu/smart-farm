package com.smartfarm.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monthly_report")
@Getter @Setter
@NoArgsConstructor
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth; // 月份首日，如 2026-04-01

    @Column(name = "water_saved_percent")
    private Double waterSavedPercent;

    @Column(name = "total_irrigation_count")
    private Integer totalIrrigationCount;

    @Column(name = "total_water_liters")
    private Double totalWaterLiters;

    @Column(name = "device_online_rate")
    private Double deviceOnlineRate;

    @Column(name = "alert_count")
    private Integer alertCount;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

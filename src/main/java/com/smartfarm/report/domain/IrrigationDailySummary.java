package com.smartfarm.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "irrigation_daily_summary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "greenhouse_no", "summary_date"}))
@Getter @Setter
@NoArgsConstructor
public class IrrigationDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "greenhouse_no", length = 20)
    private String greenhouseNo;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "irrigation_count")
    private Integer irrigationCount = 0;

    @Column(name = "total_duration_min")
    private Integer totalDurationMin = 0;

    @Column(name = "estimated_water_liters")
    private Double estimatedWaterLiters;

    @Column(name = "avg_soil_moisture")
    private Double avgSoilMoisture;

    @Column(name = "auto_trigger_count")
    private Integer autoTriggerCount = 0;

    @Column(name = "manual_trigger_count")
    private Integer manualTriggerCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

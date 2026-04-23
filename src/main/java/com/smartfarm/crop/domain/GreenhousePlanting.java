package com.smartfarm.crop.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "greenhouse_planting")
@Getter @Setter
@NoArgsConstructor
public class GreenhousePlanting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "greenhouse_no", nullable = false, length = 20)
    private String greenhouseNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_template_id")
    private CropTemplate cropTemplate;

    @Column(name = "planting_date", nullable = false)
    private LocalDate plantingDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(name = "current_stage", length = 32)
    private String currentStage;

    @Column(length = 16)
    private String status = "ACTIVE"; // ACTIVE/HARVESTED/ABANDONED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

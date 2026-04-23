package com.smartfarm.fertigation.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 水肥配方 — 定义不同作物/阶段的施肥方案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fertigation_recipe")
public class FertigationRecipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;              // "番茄果期配方"

    private String cropType;          // tomato/cucumber/leafy
    private String growthStage;       // seedling/growth/flowering/fruiting/harvest

    private Double nitrogenRatio;     // N 比例
    private Double phosphorusRatio;   // P 比例
    private Double potassiumRatio;    // K 比例
    private Double ecTarget;          // 目标 EC 值 (mS/cm)
    private Double phTarget;          // 目标 pH 值
    private Double concentrationMlPerL; // 浓缩液 ml/L

    private Integer irrigationDurationMin; // 配合灌溉时长
    private Boolean isSystem = false;      // 系统预置

    @Column(length = 500)
    private String notes;
}

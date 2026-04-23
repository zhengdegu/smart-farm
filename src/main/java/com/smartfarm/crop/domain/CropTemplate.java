package com.smartfarm.crop.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "crop_template")
@Getter @Setter
@NoArgsConstructor
public class CropTemplate extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId; // NULL = system preset

    @Column(name = "crop_type", nullable = false, length = 32)
    private String cropType; // tomato/cucumber/leafy

    @Column(nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> stages;

    @Column(name = "is_system")
    private Boolean isSystem = false;
}

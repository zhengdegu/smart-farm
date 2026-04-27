package com.smartfarm.greenhouse.domain;

import com.smartfarm.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "greenhouse",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "greenhouse_no"}))
@Getter @Setter
@NoArgsConstructor
public class Greenhouse extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "greenhouse_no", nullable = false, length = 20)
    private String greenhouseNo;

    @Column
    private Double area;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "boundary_geojson", columnDefinition = "TEXT")
    private String boundaryGeojson;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private GreenhouseStatus status = GreenhouseStatus.ACTIVE;

    public enum GreenhouseStatus { ACTIVE, IDLE }
}

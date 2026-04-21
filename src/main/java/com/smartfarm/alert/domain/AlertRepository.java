package com.smartfarm.alert.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<Alert> findByTenantIdAndLevelOrderByCreatedAtDesc(Long tenantId, Alert.AlertLevel level, Pageable pageable);

    Page<Alert> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, Alert.AlertStatus status, Pageable pageable);

    long countByTenantIdAndStatus(Long tenantId, Alert.AlertStatus status);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.tenantId = :tid AND a.status = :status AND a.level = :level")
    long countByTenantIdAndStatusAndLevel(@Param("tid") Long tenantId,
                                          @Param("status") Alert.AlertStatus status,
                                          @Param("level") Alert.AlertLevel level);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.tenantId = :tid AND a.createdAt > :since")
    long countTodayByTenant(@Param("tid") Long tenantId, @Param("since") OffsetDateTime since);
}

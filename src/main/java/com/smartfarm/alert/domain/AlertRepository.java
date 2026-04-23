package com.smartfarm.alert.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

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

    @Query("SELECT a.level, COUNT(a) FROM Alert a WHERE a.tenantId = :tid AND a.createdAt BETWEEN :start AND :end GROUP BY a.level")
    List<Object[]> countByLevelAndDateRange(@Param("tid") Long tenantId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT a.type, COUNT(a) FROM Alert a WHERE a.tenantId = :tid AND a.createdAt BETWEEN :start AND :end GROUP BY a.type")
    List<Object[]> countByTypeAndDateRange(@Param("tid") Long tenantId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}

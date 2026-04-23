package com.smartfarm.telemetry.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, SensorDataId> {

    @Query("SELECT s FROM SensorData s WHERE s.deviceId = :deviceId ORDER BY s.time DESC LIMIT 1")
    Optional<SensorData> findLatestByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT s FROM SensorData s WHERE s.deviceId = :deviceId AND s.time BETWEEN :start AND :end ORDER BY s.time")
    List<SensorData> findHistory(@Param("deviceId") String deviceId,
                                 @Param("start") OffsetDateTime start,
                                 @Param("end") OffsetDateTime end);

    @Query("SELECT s FROM SensorData s WHERE s.tenantId = :tenantId AND s.time > :since ORDER BY s.time DESC")
    List<SensorData> findRecentByTenant(@Param("tenantId") Long tenantId, @Param("since") OffsetDateTime since);

    @Query("SELECT s.dataType, AVG(s.value), MIN(s.value), MAX(s.value) FROM SensorData s WHERE s.deviceId = :deviceId AND s.time BETWEEN :start AND :end GROUP BY s.dataType")
    List<Object[]> aggregateByDevice(@Param("deviceId") String deviceId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT s FROM SensorData s WHERE s.tenantId = :tenantId AND s.dataType = :dataType AND s.time BETWEEN :start AND :end ORDER BY s.time")
    List<SensorData> findByTenantAndType(@Param("tenantId") Long tenantId, @Param("dataType") String dataType, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}

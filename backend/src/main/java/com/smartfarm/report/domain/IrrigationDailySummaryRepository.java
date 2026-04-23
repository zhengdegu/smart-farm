package com.smartfarm.report.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface IrrigationDailySummaryRepository extends JpaRepository<IrrigationDailySummary, Long> {
    List<IrrigationDailySummary> findByTenantIdAndSummaryDateBetween(Long tenantId, LocalDate start, LocalDate end);
    List<IrrigationDailySummary> findByTenantIdAndGreenhouseNoAndSummaryDateBetween(Long tenantId, String greenhouseNo, LocalDate start, LocalDate end);

    @Query("SELECT SUM(s.estimatedWaterLiters) FROM IrrigationDailySummary s WHERE s.tenantId = :tenantId AND s.summaryDate BETWEEN :start AND :end")
    Double sumWaterByTenantAndDateRange(Long tenantId, LocalDate start, LocalDate end);
}

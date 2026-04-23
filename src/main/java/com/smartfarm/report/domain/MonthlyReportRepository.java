package com.smartfarm.report.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    List<MonthlyReport> findByTenantIdOrderByReportMonthDesc(Long tenantId);
    Optional<MonthlyReport> findByTenantIdAndReportMonth(Long tenantId, LocalDate reportMonth);
}

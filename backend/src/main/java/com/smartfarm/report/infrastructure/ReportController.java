package com.smartfarm.report.infrastructure;

import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.report.domain.MonthlyReport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/irrigation/daily")
    public List<IrrigationDailySummary> getDailyStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String greenhouseNo) {
        return reportService.getDailyStats(tenantId, start, end, greenhouseNo);
    }

    @GetMapping("/irrigation/monthly")
    public Map<String, Object> getMonthlyStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return reportService.getMonthlyStats(tenantId, month);
    }

    @GetMapping("/water-saving")
    public Map<String, Object> getWaterSaving(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "500") Double manualWaterPerDay) {
        return reportService.getWaterSavingComparison(tenantId, start, end, manualWaterPerDay);
    }

    @GetMapping("/alerts/stats")
    public Map<String, Object> getAlertStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return reportService.getAlertStats(tenantId, start, end);
    }

    @GetMapping("/monthly-reports")
    public List<MonthlyReport> getMonthlyReports(@RequestParam(defaultValue = "1") Long tenantId) {
        return reportService.getMonthlyReports(tenantId);
    }
}

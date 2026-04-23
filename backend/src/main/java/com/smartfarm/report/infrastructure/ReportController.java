package com.smartfarm.report.infrastructure;

import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.report.domain.MonthlyReport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "报表统计", description = "灌溉统计、节水分析、告警统计、月报")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/irrigation/daily")
    @Operation(summary = "每日灌溉统计")
    public List<IrrigationDailySummary> getDailyStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String greenhouseNo) {
        return reportService.getDailyStats(tenantId, start, end, greenhouseNo);
    }

    @GetMapping("/irrigation/monthly")
    @Operation(summary = "月度灌溉汇总")
    public Map<String, Object> getMonthlyStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return reportService.getMonthlyStats(tenantId, month);
    }

    @GetMapping("/water-saving")
    @Operation(summary = "节水效果对比")
    public Map<String, Object> getWaterSaving(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "500") Double manualWaterPerDay) {
        return reportService.getWaterSavingComparison(tenantId, start, end, manualWaterPerDay);
    }

    @GetMapping("/alerts/stats")
    @Operation(summary = "告警统计报表")
    public Map<String, Object> getAlertStats(
            @RequestParam(defaultValue = "1") Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return reportService.getAlertStats(tenantId, start, end);
    }

    @GetMapping("/monthly-reports")
    @Operation(summary = "月报列表")
    public List<MonthlyReport> getMonthlyReports(@RequestParam(defaultValue = "1") Long tenantId) {
        return reportService.getMonthlyReports(tenantId);
    }
}

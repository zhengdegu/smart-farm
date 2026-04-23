package com.smartfarm.report.application;

import com.smartfarm.alert.domain.AlertRepository;
import com.smartfarm.device.domain.Device;
import com.smartfarm.device.domain.DeviceRepository;
import com.smartfarm.irrigation.domain.CommandLog;
import com.smartfarm.irrigation.domain.CommandLogRepository;
import com.smartfarm.report.domain.*;
import com.smartfarm.telemetry.domain.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final IrrigationDailySummaryRepository dailyRepo;
    private final MonthlyReportRepository monthlyRepo;
    private final CommandLogRepository commandLogRepo;
    private final DeviceRepository deviceRepo;
    private final AlertRepository alertRepo;

    /**
     * 查询灌溉日统计
     */
    public List<IrrigationDailySummary> getDailyStats(Long tenantId, LocalDate start, LocalDate end, String greenhouseNo) {
        if (greenhouseNo != null && !greenhouseNo.isEmpty()) {
            return dailyRepo.findByTenantIdAndGreenhouseNoAndSummaryDateBetween(tenantId, greenhouseNo, start, end);
        }
        return dailyRepo.findByTenantIdAndSummaryDateBetween(tenantId, start, end);
    }

    /**
     * 查询月度统计汇总
     */
    public Map<String, Object> getMonthlyStats(Long tenantId, LocalDate month) {
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());

        List<IrrigationDailySummary> dailies = dailyRepo.findByTenantIdAndSummaryDateBetween(tenantId, start, end);

        int totalCount = dailies.stream().mapToInt(d -> d.getIrrigationCount() != null ? d.getIrrigationCount() : 0).sum();
        int totalDuration = dailies.stream().mapToInt(d -> d.getTotalDurationMin() != null ? d.getTotalDurationMin() : 0).sum();
        double totalWater = dailies.stream().mapToDouble(d -> d.getEstimatedWaterLiters() != null ? d.getEstimatedWaterLiters() : 0).sum();
        int autoCount = dailies.stream().mapToInt(d -> d.getAutoTriggerCount() != null ? d.getAutoTriggerCount() : 0).sum();
        int manualCount = dailies.stream().mapToInt(d -> d.getManualTriggerCount() != null ? d.getManualTriggerCount() : 0).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", start);
        result.put("totalIrrigationCount", totalCount);
        result.put("totalDurationMin", totalDuration);
        result.put("totalWaterLiters", totalWater);
        result.put("autoTriggerCount", autoCount);
        result.put("manualTriggerCount", manualCount);
        result.put("autoRate", totalCount > 0 ? (double) autoCount / totalCount * 100 : 0);
        return result;
    }

    /**
     * 节水率对比
     */
    public Map<String, Object> getWaterSavingComparison(Long tenantId, LocalDate start, LocalDate end, Double manualWaterPerDay) {
        Double systemWater = dailyRepo.sumWaterByTenantAndDateRange(tenantId, start, end);
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        double manualTotal = manualWaterPerDay * days;
        double saved = manualTotal > 0 ? (manualTotal - (systemWater != null ? systemWater : 0)) / manualTotal * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", Map.of("start", start, "end", end));
        result.put("systemWaterLiters", systemWater != null ? systemWater : 0);
        result.put("manualEstimateLiters", manualTotal);
        result.put("waterSavedPercent", Math.max(0, saved));
        return result;
    }

    /**
     * 告警统计
     */
    public Map<String, Object> getAlertStats(Long tenantId, LocalDate start, LocalDate end) {
        OffsetDateTime startTime = start.atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(8));
        OffsetDateTime endTime = end.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(8));

        List<Object[]> byLevel = alertRepo.countByLevelAndDateRange(tenantId, startTime, endTime);
        List<Object[]> byType = alertRepo.countByTypeAndDateRange(tenantId, startTime, endTime);

        Map<String, Long> levelStats = new LinkedHashMap<>();
        for (Object[] row : byLevel) levelStats.put(row[0].toString(), (Long) row[1]);

        Map<String, Long> typeStats = new LinkedHashMap<>();
        for (Object[] row : byType) typeStats.put(row[0].toString(), (Long) row[1]);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("period", Map.of("start", start, "end", end));
        result.put("byLevel", levelStats);
        result.put("byType", typeStats);
        result.put("total", levelStats.values().stream().mapToLong(Long::longValue).sum());
        return result;
    }

    /**
     * 获取月度报告列表
     */
    public List<MonthlyReport> getMonthlyReports(Long tenantId) {
        return monthlyRepo.findByTenantIdOrderByReportMonthDesc(tenantId);
    }

    /**
     * 每日凌晨1点自动汇总前一天的灌溉数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void generateDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成 {} 灌溉日统计", yesterday);

        OffsetDateTime dayStart = yesterday.atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(8));
        OffsetDateTime dayEnd = yesterday.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(8));

        List<CommandLog> commands = commandLogRepo.findByStatusInAndCreatedAtBefore(
                List.of(CommandLog.CommandStatus.EXECUTED), dayEnd);

        Map<String, IrrigationDailySummary> summaries = new LinkedHashMap<>();
        for (CommandLog cmd : commands) {
            if (cmd.getCreatedAt().isBefore(dayStart)) continue;
            if (cmd.getAction() != CommandLog.CommandAction.OPEN_VALVE) continue;

            String key = cmd.getTenantId() + ":" + cmd.getDeviceId();
            IrrigationDailySummary s = summaries.computeIfAbsent(key, k -> {
                IrrigationDailySummary ns = new IrrigationDailySummary();
                ns.setTenantId(cmd.getTenantId());
                ns.setSummaryDate(yesterday);
                ns.setIrrigationCount(0);
                ns.setTotalDurationMin(0);
                ns.setEstimatedWaterLiters(0.0);
                ns.setAutoTriggerCount(0);
                ns.setManualTriggerCount(0);
                return ns;
            });

            s.setIrrigationCount(s.getIrrigationCount() + 1);
            int duration = cmd.getParams() != null && cmd.getParams().containsKey("duration_min")
                    ? ((Number) cmd.getParams().get("duration_min")).intValue() : 30;
            s.setTotalDurationMin(s.getTotalDurationMin() + duration);
            s.setEstimatedWaterLiters(s.getEstimatedWaterLiters() + duration * 10.0);

            if (cmd.getTriggeredBy() != null && cmd.getTriggeredBy().startsWith("AUTO:")) {
                s.setAutoTriggerCount(s.getAutoTriggerCount() + 1);
            } else {
                s.setManualTriggerCount(s.getManualTriggerCount() + 1);
            }
        }

        for (IrrigationDailySummary s : summaries.values()) {
            dailyRepo.save(s);
        }
        log.info("灌溉日统计生成完成: {} 条记录", summaries.size());
    }
}

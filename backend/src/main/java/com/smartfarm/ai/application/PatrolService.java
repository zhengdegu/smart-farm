package com.smartfarm.ai.application;

import com.smartfarm.ai.domain.*;
import com.smartfarm.alert.application.AlertService;
import com.smartfarm.crop.application.GreenhousePlantingService;
import com.smartfarm.crop.domain.GreenhousePlanting;
import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.shared.application.WeatherService;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * AI 巡检 Agent (L2) — 定时巡检温室状态，主动发现异常
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolService {

    private final LlmClient llmClient;
    private final PatrolLogRepository patrolLogRepo;
    private final TelemetryService telemetryService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final GreenhousePlantingService plantingService;
    private final WeatherService weatherService;

    private static final Long DEFAULT_TENANT = 1L;

    private static final String PATROL_PROMPT = """
            你是温室灌溉巡检专家。分析以下数据，判断是否有异常或需要关注的情况。
            
            规则：
            1. 如果一切正常，回复 "STATUS:OK"
            2. 如果发现问题，回复 "STATUS:WARNING" 或 "STATUS:CRITICAL"，然后用一句话描述问题和建议
            3. 用农户听得懂的语言
            4. 给出具体数字和操作建议
            """;

    /**
     * 每30分钟巡检一次环境趋势
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    public void trendCheck() {
        try {
            List<SensorData> recent = telemetryService.getRecentByTenant(DEFAULT_TENANT, 60);
            if (recent.isEmpty()) return;

            // 按类型分组取最新值
            Map<String, Double> latestValues = new LinkedHashMap<>();
            for (SensorData sd : recent) {
                latestValues.putIfAbsent(sd.getDataType(), sd.getValue());
            }

            String context = String.format("""
                    巡检类型: 环境趋势检查
                    当前时间: %s
                    天气: %s
                    传感器数据: %s
                    """, OffsetDateTime.now(), weatherService.getWeatherDescription(), latestValues);

            LlmClient.LlmResponse resp = llmClient.chat(PATROL_PROMPT, context);

            PatrolLog.Severity severity = PatrolLog.Severity.INFO;
            PatrolLog.ActionTaken action = PatrolLog.ActionTaken.IGNORED;

            if (resp.content().contains("STATUS:CRITICAL")) {
                severity = PatrolLog.Severity.CRITICAL;
                action = PatrolLog.ActionTaken.NOTIFIED;
                log.warn("巡检发现严重问题: {}", resp.content());
            } else if (resp.content().contains("STATUS:WARNING")) {
                severity = PatrolLog.Severity.WARNING;
                action = PatrolLog.ActionTaken.NOTIFIED;
                log.info("巡检发现警告: {}", resp.content());
            }

            savePatrolLog("TREND_CHECK", null, resp.content(), severity, action, context);

        } catch (Exception e) {
            log.debug("趋势巡检跳过: {}", e.getMessage());
        }
    }

    /**
     * 每2小时检查设备健康
     */
    @Scheduled(fixedDelay = 7200000, initialDelay = 120000)
    public void deviceHealthCheck() {
        try {
            var devices = telemetryService.getRecentByTenant(DEFAULT_TENANT, 120);
            Map<String, OffsetDateTime> lastSeen = new LinkedHashMap<>();
            for (SensorData sd : devices) {
                lastSeen.merge(sd.getDeviceId(), sd.getTime(), (a, b) -> a.isAfter(b) ? a : b);
            }

            StringBuilder context = new StringBuilder("设备健康检查:\n");
            for (var entry : lastSeen.entrySet()) {
                long minutesAgo = java.time.Duration.between(entry.getValue(), OffsetDateTime.now()).toMinutes();
                context.append(String.format("- %s: 最后上报 %d 分钟前%s\n",
                        entry.getKey(), minutesAgo, minutesAgo > 10 ? " [可能离线]" : ""));
            }

            LlmClient.LlmResponse resp = llmClient.chat(PATROL_PROMPT, context.toString());

            PatrolLog.Severity severity = resp.content().contains("STATUS:CRITICAL") ? PatrolLog.Severity.CRITICAL :
                    resp.content().contains("STATUS:WARNING") ? PatrolLog.Severity.WARNING : PatrolLog.Severity.INFO;

            savePatrolLog("DEVICE_HEALTH", null, resp.content(), severity,
                    severity != PatrolLog.Severity.INFO ? PatrolLog.ActionTaken.NOTIFIED : PatrolLog.ActionTaken.IGNORED,
                    context.toString());

        } catch (Exception e) {
          log.debug("设备健康巡检跳过: {}", e.getMessage());
        }
    }

    /**
     * 每日20:00生成日摘要
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void dailySummary() {
        try {
            LocalDate today = LocalDate.now();
            var dailyStats = reportService.getDailyStats(DEFAULT_TENANT, today, today, null);
            var alertStats = alertService.getStats(DEFAULT_TENANT);
            var weather = weatherService.getWeatherDescription();

            int totalIrrigation = dailyStats.stream().mapToInt(IrrigationDailySummary::getIrrigationCount).sum();
            double totalWater = dailyStats.stream().mapToDouble(IrrigationDailySummary::getEstimatedWaterLiters).sum();

            String context = String.format("""
                    今日运营摘要:
                    日期: %s
                    天气: %s
                    灌溉次数: %d
                    用水量: %.1f升
                    告警统计: %s
                    请生成一段简短的今日摘要，用农户听得懂的语言。
                    """, today, weather, totalIrrigation, totalWater, alertStats);

            LlmClient.LlmResponse resp = llmClient.chat(
                    "你是温室管理助手，请生成今日运营摘要，简短、实用、通俗。", context);

            savePatrolLog("DAILY_SUMMARY", null, resp.content(), PatrolLog.Severity.INFO,
                    PatrolLog.ActionTaken.NOTIFIED, context);

            log.info("每日摘要已生成");

        } catch (Exception e) {
            log.warn("每日摘要生成失败: {}", e.getMessage());
        }
    }

    /**
     * 手动触发巡检
     */
    public PatrolLog triggerPatrol(Long tenantId, String patrolType) {
        switch (patrolType) {
            case "TREND_CHECK" -> trendCheck();
            case "DEVICE_HEALTH" -> deviceHealthCheck();
            case "DAILY_SUMMARY" -> dailySummary();
         default -> throw new RuntimeException("未知巡检类型: " + patrolType);
        }
        var logs = patrolLogRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return logs.isEmpty() ? null : logs.get(0);
    }

    public List<PatrolLog> listLogs(Long tenantId) {
        return patrolLogRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private void savePatrolLog(String type, String ghNo, String finding,
                                PatrolLog.Severity severity, PatrolLog.ActionTaken action, String context) {
        PatrolLog pl = new PatrolLog();
        pl.setTenantId(DEFAULT_TENANT);
        pl.setPatrolType(type);
        pl.setGreenhouseNo(ghNo);
        pl.setFinding(finding);
        pl.setSeverity(severity);
        pl.setActionTaken(action);
        pl.setRawContext(context);
        patrolLogRepo.save(pl);
    }
}

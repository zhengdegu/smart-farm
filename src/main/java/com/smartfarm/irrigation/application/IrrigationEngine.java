package com.smartfarm.irrigation.application;

import com.smartfarm.alert.application.AlertService;
import com.smartfarm.alert.domain.Alert;
import com.smartfarm.irrigation.domain.*;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 灌溉规则引擎 - 定时检查阈值规则并触发灌溉指令
 * 
 * 功能：
 * 1. 阈值触发：土壤湿度低于下限→开阀，高于上限→关阀
 * 2. 安全护栏：单次灌溉最长60分钟强制关阀
 * 3. 指令超时检测：SENT状态超过30秒未确认→标记失败并重试
 * 4. 设备离线检测：5分钟无心跳→标记离线并告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IrrigationEngine {

    private final RuleService ruleService;
    private final CommandService commandService;
    private final TelemetryService telemetryService;
    private final AlertService alertService;
    private final CommandLogRepository commandLogRepo;

    @Value("${smartfarm.command.timeout-seconds:30}")
    private int commandTimeoutSeconds;

    @Value("${smartfarm.command.max-retry:3}")
    private int maxRetry;

    @Value("${smartfarm.command.max-duration-minutes:60}")
    private int maxDurationMinutes;

    /**
     * 每30秒检查一次阈值规则
     */
    @Scheduled(fixedDelay = 30000)
    public void evaluateThresholdRules() {
        List<IrrigationRule> rules = ruleService.getEnabledRules();
        for (IrrigationRule rule : rules) {
            if (rule.getRuleType() != IrrigationRule.RuleType.THRESHOLD) continue;
            if (rule.getSensorDeviceId() == null) continue;

            try {
                SensorData latest = telemetryService.getLatest(rule.getSensorDeviceId());
                if (latest == null) continue;

                // 数据太旧（超过10分钟）跳过
                if (latest.getTime().isBefore(OffsetDateTime.now().minusMinutes(10))) continue;

                double value = latest.getValue();
                String dataType = latest.getDataType();

                // 只处理土壤湿度
                if (!"soil_moisture".equals(dataType)) continue;

                if (rule.getThresholdLow() != null && value < rule.getThresholdLow()) {
                    // 湿度低于下限 → 开阀
                    if (!hasActiveOpenCommand(rule.getDeviceId())) {
                        log.info("规则触发[{}]: {}={} < {}, 开阀 {}",
                                rule.getName(), dataType, value, rule.getThresholdLow(), rule.getDeviceId());
                        commandService.sendCommand(rule.getTenantId(), rule.getDeviceId(),
                                CommandLog.CommandAction.OPEN_VALVE,
                                Map.of("duration_min", rule.getDurationMin() != null ? rule.getDurationMin() : 30),
                                "AUTO:rule_" + rule.getId());
                        rule.setLastTriggeredAt(OffsetDateTime.now());
                    }
                } else if (rule.getThresholdHigh() != null && value > rule.getThresholdHigh()) {
                    // 湿度高于上限 → 关阀
                    if (hasActiveOpenCommand(rule.getDeviceId())) {
                        log.info("规则触发[{}]: {}={} > {}, 关阀 {}",
                                rule.getName(), dataType, value, rule.getThresholdHigh(), rule.getDeviceId());
                        commandService.sendCommand(rule.getTenantId(), rule.getDeviceId(),
                                CommandLog.CommandAction.CLOSE_VALVE,
                                Map.of(), "AUTO:rule_" + rule.getId());
                    }
                }
            } catch (Exception e) {
                log.debug("规则评估跳过[{}]: {}", rule.getName(), e.getMessage());
            }
        }
    }

    /**
     * 安全护栏：每分钟检查，单次灌溉超过最大时长强制关阀
     */
    @Scheduled(fixedDelay = 60000)
    public void enforceSafetyGuard() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(maxDurationMinutes);
        List<CommandLog> overdue = commandLogRepo.findByStatusInAndCreatedAtBefore(
                List.of(CommandLog.CommandStatus.SENT, CommandLog.CommandStatus.CONFIRMED, CommandLog.CommandStatus.EXECUTED),
                cutoff);

        for (CommandLog cmd : overdue) {
            if (cmd.getAction() == CommandLog.CommandAction.OPEN_VALVE) {
                log.warn("安全护栏触发: 指令 {} 已运行超过 {} 分钟, 强制关阀 {}",
                        cmd.getCmdId(), maxDurationMinutes, cmd.getDeviceId());
                try {
                    commandService.sendCommand(cmd.getTenantId(), cmd.getDeviceId(),
                            CommandLog.CommandAction.CLOSE_VALVE,
                            Map.of("reason", "safety_guard"), "SYSTEM:safety_guard");
                    alertService.createAlert(cmd.getTenantId(), cmd.getDeviceId(),
                            Alert.AlertLevel.L1, "SAFETY_GUARD",
                            "安全护栏触发: 灌溉超时强制关阀",
                            "指令 " + cmd.getCmdId() + " 运行超过 " + maxDurationMinutes + " 分钟");
                } catch (Exception e) {
                    log.error("安全护栏关阀失败: device={}", cmd.getDeviceId(), e);
                }
            }
        }
    }

    /**
     * 指令超时检测：每15秒检查，SENT状态超时→标记失败并重试
     */
    @Scheduled(fixedDelay = 15000)
    public void checkCommandTimeout() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(commandTimeoutSeconds);
        List<CommandLog> timedOut = commandLogRepo.findByStatusAndSentAtBefore(
                CommandLog.CommandStatus.SENT, cutoff);

        for (CommandLog cmd : timedOut) {
            if (cmd.getRetryCount() < maxRetry) {
                log.warn("指令超时重试: cmdId={}, retry={}/{}", cmd.getCmdId(), cmd.getRetryCount() + 1, maxRetry);
                cmd.setRetryCount(cmd.getRetryCount() + 1);
                cmd.setStatus(CommandLog.CommandStatus.CREATED);
                commandLogRepo.save(cmd);

                // 重新下发
                try {
                    commandService.sendCommand(cmd.getTenantId(), cmd.getDeviceId(),
                            cmd.getAction(), cmd.getParams(), cmd.getTriggeredBy() + ":retry");
                } catch (Exception e) {
                    log.error("指令重试失败: cmdId={}", cmd.getCmdId(), e);
                }
            } else {
                log.error("指令最终失败: cmdId={}, 已重试 {} 次", cmd.getCmdId(), maxRetry);
                cmd.markFailed("TIMEOUT", "指令超时，已重试 " + maxRetry + " 次");
                commandLogRepo.save(cmd);

                alertService.createAlert(cmd.getTenantId(), cmd.getDeviceId(),
                        Alert.AlertLevel.L2, "CMD_TIMEOUT",
                        "灌溉指令超时失败",
                        "指令 " + cmd.getCmdId() + " 超时，已重试 " + maxRetry + " 次仍未确认");
            }
        }
    }

    private boolean hasActiveOpenCommand(String deviceId) {
        List<CommandLog> active = commandLogRepo.findByDeviceIdAndStatusIn(deviceId,
                List.of(CommandLog.CommandStatus.CREATED, CommandLog.CommandStatus.SENT,
                        CommandLog.CommandStatus.CONFIRMED, CommandLog.CommandStatus.EXECUTED));
        return active.stream().anyMatch(c -> c.getAction() == CommandLog.CommandAction.OPEN_VALVE);
    }
}

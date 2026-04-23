package com.smartfarm.irrigation.application;

import com.smartfarm.irrigation.domain.*;
import com.smartfarm.shared.exception.BusinessException;
import com.smartfarm.shared.mqtt.MqttPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandService {

    private final CommandLogRepository commandLogRepository;
    private final MqttPublisher mqttPublisher;

    @Value("${smartfarm.tenant-id}")
    private String defaultTenantId;

    @Transactional
    public CommandLog sendCommand(Long tenantId, String deviceId,
                                   CommandLog.CommandAction action, Map<String, Object> params,
                                   String triggeredBy) {
        CommandLog cmd = new CommandLog();
        cmd.setCmdId("cmd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        cmd.setTenantId(tenantId);
        cmd.setDeviceId(deviceId);
        cmd.setAction(action);
        cmd.setParams(params);
        cmd.setTriggeredBy(triggeredBy);
        commandLogRepository.save(cmd);

        // 通过 MQTT 下发指令
        String topic = String.format("/%s/valve/%s/command", defaultTenantId, deviceId);
        Map<String, Object> payload = Map.of(
                "cmd_id", cmd.getCmdId(),
                "action", action.name(),
                "params", params != null ? params : Map.of(),
                "timestamp", System.currentTimeMillis() / 1000
        );
        mqttPublisher.publish(topic, payload);
        cmd.markSent();
        commandLogRepository.save(cmd);

        log.info("指令已下发: cmdId={}, device={}, action={}", cmd.getCmdId(), deviceId, action);
        return cmd;
    }

    @Transactional
    public int emergencyStop(Long tenantId) {
        // 查找所有活跃的阀门指令，发送关闭
        List<CommandLog> active = commandLogRepository.findByStatusIn(
                List.of(CommandLog.CommandStatus.CREATED, CommandLog.CommandStatus.SENT, CommandLog.CommandStatus.CONFIRMED));
        int count = 0;
        for (CommandLog cmd : active) {
            if (cmd.getTenantId().equals(tenantId) && cmd.getAction() == CommandLog.CommandAction.OPEN_VALVE) {
                sendCommand(tenantId, cmd.getDeviceId(), CommandLog.CommandAction.CLOSE_VALVE,
                        Map.of(), "EMERGENCY_STOP");
                count++;
            }
        }
        log.warn("紧急停止: 已关闭 {} 个阀门", count);
        return count;
    }

    public CommandLog getCommand(String cmdId) {
        return commandLogRepository.findByCmdId(cmdId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CMD_NOT_FOUND", "指令不存在: " + cmdId));
    }

    public List<CommandLog> listCommands(Long tenantId) {
        return commandLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void handleAck(String cmdId, String status, Map<String, Object> result, String errorCode, String errorMsg) {
        CommandLog cmd = getCommand(cmdId);
        switch (status) {
            case "CONFIRMED" -> cmd.markConfirmed();
            case "EXECUTED" -> cmd.markExecuted(result);
            case "FAILED" -> cmd.markFailed(errorCode, errorMsg);
            default -> log.warn("未知指令状态: {}", status);
        }
        commandLogRepository.save(cmd);
    }
}

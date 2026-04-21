package com.smartfarm.shared.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfarm.alert.application.AlertService;
import com.smartfarm.alert.domain.Alert;
import com.smartfarm.device.application.DeviceService;
import com.smartfarm.device.domain.Device;
import com.smartfarm.irrigation.application.CommandService;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageRouter {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final TelemetryService telemetryService;
    private final CommandService commandService;
    private final DeviceService deviceService;
    private final AlertService alertService;

    @Value("${smartfarm.tenant-id}")
    private String tenantId;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeTopics() {
        try {
            // 订阅传感器数据
            String sensorTopic = "/" + tenantId + "/sensor/+/telemetry";
            mqttClient.subscribe(sensorTopic, 1, (topic, msg) -> handleSensorData(topic, msg));
            log.info("已订阅: {}", sensorTopic);

            // 订阅阀门 ACK
            String ackTopic = "/" + tenantId + "/valve/+/command_ack";
            mqttClient.subscribe(ackTopic, 1, (topic, msg) -> handleCommandAck(topic, msg));
            log.info("已订阅: {}", ackTopic);

        } catch (MqttException e) {
            log.error("MQTT 订阅失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSensorData(String topic, MqttMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String deviceId = (String) payload.get("device_id");
            String dataType = (String) payload.get("data_type");
            Number value = (Number) payload.get("value");
            String unit = (String) payload.get("unit");

            SensorData data = new SensorData();
            data.setTime(OffsetDateTime.now());
            data.setDeviceId(deviceId);
            data.setTenantId(1L); // TODO: 从 topic 解析 tenant
            data.setDataType(dataType);
            data.setValue(value.doubleValue());
            data.setUnit(unit);
            data.setQuality(100);
            data.setRawPayload(payload);

            telemetryService.ingest(data);

            // 更新设备在线状态
            try {
                deviceService.updateStatus(deviceId, Device.DeviceStatus.ONLINE);
            } catch (Exception ignored) {}

            // 检查异常阈值
            checkThreshold(deviceId, dataType, value.doubleValue());

        } catch (Exception e) {
            log.error("处理传感器数据失败: topic={}", topic, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCommandAck(String topic, MqttMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String cmdId = (String) payload.get("cmd_id");
            String status = (String) payload.get("status");
            String error = (String) payload.getOrDefault("error", "");

            Map<String, Object> result = (Map<String, Object>) payload.get("result");
            commandService.handleAck(cmdId, status, result, error, error);

            log.info("指令 ACK: cmdId={}, status={}", cmdId, status);

            // 指令失败时创建告警
            if ("FAILED".equals(status)) {
                String deviceId = (String) payload.get("device_id");
                alertService.createAlert(1L, deviceId, Alert.AlertLevel.L2,
                        "CMD_FAILED", "灌溉指令执行失败",
                        "指令 " + cmdId + " 执行失败: " + error);
            }
        } catch (Exception e) {
            log.error("处理指令 ACK 失败: topic={}", topic, e);
        }
    }

    private void checkThreshold(String deviceId, String dataType, double value) {
        // 简单阈值检查，后续由规则引擎接管
        boolean anomaly = false;
        String title = "";
        if ("soil_moisture".equals(dataType) && value < 20) {
            anomaly = true;
            title = "土壤湿度过低: " + value + "%";
        } else if ("soil_moisture".equals(dataType) && value > 90) {
            anomaly = true;
            title = "土壤湿度过高: " + value + "%";
        } else if ("air_temp".equals(dataType) && (value < 5 || value > 40)) {
            anomaly = true;
            title = "气温异常: " + value + "°C";
        }
        if (anomaly) {
            alertService.createAlert(1L, deviceId, Alert.AlertLevel.L2,
                    "THRESHOLD_EXCEEDED", title, dataType + "=" + value);
        }
    }
}

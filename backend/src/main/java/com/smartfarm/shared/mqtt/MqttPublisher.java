package com.smartfarm.shared.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttPublisher {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.default-qos}")
    private int defaultQos;

    public void publish(String topic, Object payload) {
        publish(topic, payload, defaultQos);
    }

    public void publish(String topic, Object payload, int qos) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MqttMessage msg = new MqttMessage(json.getBytes());
            msg.setQos(qos);
            mqttClient.publish(topic, msg);
            log.debug("MQTT 发布: {} -> {}", topic, json);
        } catch (MqttException e) {
            log.error("MQTT 发布失败: topic={}", topic, e);
            throw new RuntimeException("MQTT 发布失败", e);
        } catch (Exception e) {
            log.error("消息序列化失败", e);
            throw new RuntimeException("消息序列化失败", e);
        }
    }
}

package com.smartfarm.shared.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Bean(destroyMethod = "disconnect")
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(broker, clientId);
        MqttConnectionOptions options = new MqttConnectionOptions();
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
            options.setPassword(password.getBytes());
        }
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        options.setKeepAliveInterval(30);

        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse r) {
                log.warn("MQTT 断开连接: {}", r.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException e) {
                log.error("MQTT 错误", e);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // 由 MqttMessageRouter 处理
            }

            @Override
            public void deliveryComplete(IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                log.info("MQTT {}连接成功: {}", reconnect ? "重新" : "", serverURI);
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        try {
            client.connect(options);
            log.info("MQTT 已连接: {}", broker);
        } catch (MqttException e) {
            log.error("MQTT 连接失败: {}", e.getMessage());
        }
        return client;
    }
}

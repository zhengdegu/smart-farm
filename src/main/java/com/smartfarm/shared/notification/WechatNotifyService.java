package com.smartfarm.shared.notification;

import com.smartfarm.alert.domain.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信服务号模板消息通知
 * 
 * 告警级别对应：
 * - L1 紧急：立即推送，红色标题
 * - L2 重要：立即推送
 * - L3 一般：汇总后推送（每小时一次）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatNotifyService {

    private final RestTemplate restTemplate;

    @Value("${wechat.app-id:}")
    private String appId;

    @Value("${wechat.app-secret:}")
    private String appSecret;

    @Value("${wechat.template-id.alert:}")
    private String alertTemplateId;

    @Value("${wechat.enabled:false}")
    private boolean enabled;

    private String accessToken;
    private long tokenExpireAt;

    @Async
    public void sendAlertNotification(Alert alert, String openId) {
        if (!enabled || openId == null || openId.isEmpty()) {
            log.debug("微信通知跳过: enabled={}, openId={}", enabled, openId);
            return;
        }

        try {
            String token = getAccessToken();
            String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token;

            Map<String, Object> body = new HashMap<>();
            body.put("touser", openId);
            body.put("template_id", alertTemplateId);

            Map<String, Object> data = new HashMap<>();
            data.put("first", Map.of("value", getAlertPrefix(alert.getLevel()) + alert.getTitle()));
            data.put("keyword1", Map.of("value", alert.getType(), "color", "#173177"));
            data.put("keyword2", Map.of("value", alert.getDeviceId() != null ? alert.getDeviceId() : "系统"));
            data.put("keyword3", Map.of("value", alert.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            data.put("remark", Map.of("value", alert.getContent() != null ? alert.getContent() : ""));
            body.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (resp.getBody() != null && Integer.valueOf(0).equals(resp.getBody().get("errcode"))) {
                log.info("微信通知发送成功: alertId={}, openId={}", alert.getId(), openId);
                alert.setNotified(true);
            } else {
                log.warn("微信通知发送失败: {}", resp.getBody());
            }
        } catch (Exception e) {
            log.error("微信通知异常: alertId={}", alert.getId(), e);
        }
    }

    private String getAlertPrefix(Alert.AlertLevel level) {
        return switch (level) {
            case L1 -> "🔴 紧急告警 | ";
            case L2 -> "🟡 重要告警 | ";
            case L3 -> "🔵 一般通知 | ";
        };
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return accessToken;
        }
        String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appId, appSecret);
        Map resp = restTemplate.getForObject(url, Map.class);
        if (resp != null && resp.containsKey("access_token")) {
            accessToken = (String) resp.get("access_token");
            tokenExpireAt = System.currentTimeMillis() + ((Number) resp.get("expires_in")).longValue() * 1000 - 60000;
            return accessToken;
        }
        throw new RuntimeException("获取微信 access_token 失败: " + resp);
    }
}

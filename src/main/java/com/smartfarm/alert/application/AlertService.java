package com.smartfarm.alert.application;

import com.smartfarm.alert.domain.Alert;
import com.smartfarm.alert.domain.AlertRepository;
import com.smartfarm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public Page<Alert> listAlerts(Long tenantId, Alert.AlertLevel level, Alert.AlertStatus status, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        if (level != null) return alertRepository.findByTenantIdAndLevelOrderByCreatedAtDesc(tenantId, level, pageable);
        if (status != null) return alertRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
        return alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional
    public void acknowledge(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", "告警不存在"));
        alert.acknowledge(userId);
        alertRepository.save(alert);
    }

    public Map<String, Object> getStats(Long tenantId) {
        OffsetDateTime todayStart = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.ofHours(8));
        return Map.of(
                "pending_l1", alertRepository.countByTenantIdAndStatusAndLevel(tenantId, Alert.AlertStatus.PENDING, Alert.AlertLevel.L1),
                "pending_l2", alertRepository.countByTenantIdAndStatusAndLevel(tenantId, Alert.AlertStatus.PENDING, Alert.AlertLevel.L2),
                "pending_l3", alertRepository.countByTenantIdAndStatusAndLevel(tenantId, Alert.AlertStatus.PENDING, Alert.AlertLevel.L3),
                "today_total", alertRepository.countTodayByTenant(tenantId, todayStart)
        );
    }

    @Transactional
    public Alert createAlert(Long tenantId, String deviceId, Alert.AlertLevel level,
                              String type, String title, String content) {
        Alert alert = new Alert();
        alert.setTenantId(tenantId);
        alert.setDeviceId(deviceId);
        alert.setLevel(level);
        alert.setType(type);
        alert.setTitle(title);
        alert.setContent(content);
        return alertRepository.save(alert);
    }
}

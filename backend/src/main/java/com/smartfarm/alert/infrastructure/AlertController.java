package com.smartfarm.alert.infrastructure;

import com.smartfarm.alert.application.AlertService;
import com.smartfarm.alert.domain.Alert;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警查询、确认、统计")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "告警列表", description = "支持按级别/状态筛选，分页")
    public Map<String, Object> list(
            @RequestParam(required = false) Alert.AlertLevel level,
            @RequestParam(required = false) Alert.AlertStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Alert> result = alertService.listAlerts(SecurityUtils.currentTenantId(), level, status, page, size);
        return Map.of(
                "total", result.getTotalElements(),
                "page", result.getNumber() + 1,
                "size", result.getSize(),
                "items", result.getContent()
        );
    }

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "确认告警")
    public void acknowledge(@PathVariable Long alertId) {
        alertService.acknowledge(alertId, SecurityUtils.currentUserId());
    }

    @GetMapping("/stats")
    @Operation(summary = "告警统计")
    public Map<String, Object> stats() {
        return alertService.getStats(SecurityUtils.currentTenantId());
    }
}

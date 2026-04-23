package com.smartfarm.telemetry.infrastructure;

import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @GetMapping("/{deviceId}/latest")
    public SensorData latest(@PathVariable String deviceId) {
        return telemetryService.getLatest(deviceId);
    }

    @GetMapping("/{deviceId}/history")
    public List<SensorData> history(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return telemetryService.getHistory(deviceId, start, end);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Long tenantId = SecurityUtils.currentTenantId();
        List<SensorData> recent = telemetryService.getRecentByTenant(tenantId, 30);
        return Map.of("sensors", recent);
    }

    @GetMapping("/{deviceId}/aggregate")
    public List<Map<String, Object>> aggregate(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return telemetryService.aggregate(deviceId, start, end).stream()
                .map(row -> Map.<String, Object>of(
                        "dataType", row[0], "avg", row[1], "min", row[2], "max", row[3]))
                .toList();
    }

    @GetMapping("/trend")
    public List<SensorData> trend(
            @RequestParam String dataType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return telemetryService.getByTenantAndType(SecurityUtils.currentTenantId(), dataType, start, end);
    }
}

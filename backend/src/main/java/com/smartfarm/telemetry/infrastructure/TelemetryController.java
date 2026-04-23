package com.smartfarm.telemetry.infrastructure;

import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import com.smartfarm.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
@Tag(name = "遥测数据", description = "传感器数据查询、历史、聚合、趋势")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @GetMapping("/{deviceId}/latest")
    @Operation(summary = "最新遥测数据")
    public SensorData latest(@PathVariable String deviceId) {
        return telemetryService.getLatest(deviceId);
    }

    @GetMapping("/{deviceId}/history")
    @Operation(summary = "历史遥测数据")
    public List<SensorData> history(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return telemetryService.getHistory(deviceId, start, end);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘数据")
    public Map<String, Object> dashboard() {
        Long tenantId = SecurityUtils.currentTenantId();
        List<SensorData> recent = telemetryService.getRecentByTenant(tenantId, 30);
        return Map.of("sensors", recent);
    }

    @GetMapping("/{deviceId}/aggregate")
    @Operation(summary = "数据聚合统计", description = "按数据类型返回平均/最小/最大值")
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
    @Operation(summary = "数据趋势", description = "按数据类型查询时间范围内的趋势数据")
    public List<SensorData> trend(
            @RequestParam String dataType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return telemetryService.getByTenantAndType(SecurityUtils.currentTenantId(), dataType, start, end);
    }
}

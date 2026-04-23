package com.smartfarm.shared.infrastructure;

import com.smartfarm.alert.application.AlertService;
import com.smartfarm.alert.domain.Alert;
import com.smartfarm.crop.application.GreenhousePlantingService;
import com.smartfarm.crop.domain.GreenhousePlanting;
import com.smartfarm.device.application.DeviceService;
import com.smartfarm.device.domain.Device;
import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.telemetry.application.TelemetryService;
import com.smartfarm.telemetry.domain.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.smartfarm.shared.application.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 3D 数据大屏专用聚合接口 - 免鉴权
 */
@RestController
@RequestMapping("/api/v1/screen")
@RequiredArgsConstructor
@Tag(name = "大屏数据", description = "3D数据大屏聚合接口（免鉴权）")
public class ScreenController {

    private static final Long DEFAULT_TENANT = 1L;

    private final DeviceService deviceService;
    private final TelemetryService telemetryService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final GreenhousePlantingService plantingService;
    private final WeatherService weatherService;

    @GetMapping("/data")
    @Operation(summary = "大屏全量数据", description = "一次返回设备概览、实时环境、告警、灌溉统计、节水效果、种植进度")
    public Map<String, Object> screenData() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 设备概览
        List<Device> devices = deviceService.listDevices(DEFAULT_TENANT, null, null);
        long online = devices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.ONLINE).count();
        long sensors = devices.stream().filter(d -> d.getDeviceType() == Device.DeviceType.SENSOR).count();
        long valves = devices.stream().filter(d -> d.getDeviceType() == Device.DeviceType.VALVE).count();
        result.put("deviceStats", Map.of(
                "onlineDevices", online,
                "offlineDevices", devices.size() - online,
                "totalSensors", sensors,
                "totalValves", valves
        ));

        // 2. 实时环境 - 每种类型取最新一条
        List<SensorData> recent = telemetryService.getRecentByTenant(DEFAULT_TENANT, 60);
        Map<String, SensorData> latestByType = new LinkedHashMap<>();
        for (SensorData sd : recent) {
            latestByType.putIfAbsent(sd.getDataType(), sd);
        }
        List<Map<String, Object>> envList = new ArrayList<>();
        Map<String, String[]> envMeta = new LinkedHashMap<>();
        envMeta.put("soil_moisture", new String[]{"土壤湿度", "%", "#409eff"});
        envMeta.put("soil_temp", new String[]{"土壤温度", "°C", "#e6a23c"});
        envMeta.put("air_temp", new String[]{"空气温度", "°C", "#f56c6c"});
        envMeta.put("air_humidity", new String[]{"空气湿度", "%", "#67c23a"});
        for (var entry : envMeta.entrySet()) {
            SensorData sd = latestByType.get(entry.getKey());
            String[] meta = entry.getValue();
            envList.add(Map.of(
                    "label", meta[0], "unit", meta[1], "color", meta[2],
                    "value", sd != null ? sd.getValue() : 0.0
            ));
        }
        result.put("envData", envList);

        // 3. 告警 - 最新 10 条未确认
        Page<Alert> alertPage = alertService.listAlerts(DEFAULT_TENANT, null, Alert.AlertStatus.PENDING, 1, 10);
        result.put("alerts", alertPage.getContent().stream().map(a -> Map.of(
                "id", a.getId(),
                "level", a.getLevel().name(),
                "title", a.getTitle()
        )).toList());

        // 4. 灌溉统计 - 近 7 天
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<IrrigationDailySummary> dailyStats = reportService.getDailyStats(DEFAULT_TENANT, weekAgo, today, null);
        Map<LocalDate, Integer> dailyMap = dailyStats.stream()
                .collect(Collectors.toMap(IrrigationDailySummary::getSummaryDate, IrrigationDailySummary::getIrrigationCount, Integer::sum));
        List<String> irrigDates = new ArrayList<>();
        List<Integer> irrigCounts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekAgo.plusDays(i);
            irrigDates.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            irrigCounts.add(dailyMap.getOrDefault(d, 0));
        }
        result.put("irrigationChart", Map.of("dates", irrigDates, "counts", irrigCounts));

        // 5. 节水效果
        Map<String, Object> waterSaving = reportService.getWaterSavingComparison(DEFAULT_TENANT, weekAgo, today, 500.0);
        result.put("waterSaving", waterSaving);

        // 6. 种植进度
        List<GreenhousePlanting> plantings = plantingService.listByTenant(DEFAULT_TENANT);
        List<Map<String, Object>> plantList = new ArrayList<>();
        for (GreenhousePlanting p : plantings) {
            try {
                Map<String, Object> info = plantingService.getStageInfo(p.getId());
                // currentStage is a Map with "name", "stage", etc.
                Object cs = info.get("currentStage");
                String stageName = "未知";
                if (cs instanceof Map) {
                    Object n = ((Map<?, ?>) cs).get("name");
                    if (n != null) stageName = n.toString();
                } else if (cs instanceof String) {
                    stageName = (String) cs;
                }
                // calculate progress
                long daysSince = info.get("daysSincePlanting") != null ? ((Number) info.get("daysSincePlanting")).longValue() : 0;
                int totalDays = 0;
                if (p.getPlantingDate() != null && p.getExpectedHarvestDate() != null) {
                    totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(p.getPlantingDate(), p.getExpectedHarvestDate());
                }
                int progress = totalDays > 0 ? (int) Math.min(100, daysSince * 100 / totalDays) : 0;

                plantList.add(Map.of(
                        "no", p.getGreenhouseNo(),
                        "crop", info.getOrDefault("templateName", "未知"),
                        "stage", stageName,
                        "progress", progress
                ));
            } catch (Exception ignored) {}
        }
        result.put("plantings", plantList);

        // 7. 天气数据
        WeatherService.WeatherData weather = weatherService.getCurrentWeather();
        WeatherService.WeatherForecast wForecast = weatherService.getForecast();
        if (weather != null) {
            Map<String, Object> weatherMap = new LinkedHashMap<>();
            weatherMap.put("temperature", weather.getTemperature());
            weatherMap.put("humidity", weather.getHumidity());
            weatherMap.put("precipitation", weather.getPrecipitation());
            weatherMap.put("isRaining", weather.isRaining());
            weatherMap.put("description", weatherService.getWeatherDescription());
            weatherMap.put("irrigationFactor", weatherService.getIrrigationAdjustmentFactor());
            if (wForecast != null) {
                weatherMap.put("willRainSoon", wForecast.isWillRainSoon());
                weatherMap.put("precipitationNext6h", wForecast.getPrecipitationNext6h());
                weatherMap.put("maxTempToday", wForecast.getMaxTempToday());
            }
            result.put("weather", weatherMap);
        }

        return result;
    }
}
